package com.hrms.service;

import com.hrms.dto.response.PayslipResponse;
import com.hrms.entity.*;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates monthly payslip generation:
 *   1. Compute earnings/deductions from the employee's currently assigned salary structure
 *   2. Compute the leave summary for the period (days used that month + running balance)
 *   3. Persist the Payslip row
 *   4. Render the PDF
 *   5. Email it, logging success/failure
 *   6. Roll all of the above up into a PayrollRun so the dashboard can report on it
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipService {

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeSalaryHistoryRepository salaryHistoryRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final EmailService emailService;

    /** Generates + emails payslips for every active employee for the given period. */
    @Transactional
    public PayrollRun runMonthlyPayroll(int month, int year) {
        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();

        PayrollRun run = payrollRunRepository.findByPayPeriodMonthAndPayPeriodYear(month, year)
                .orElseGet(() -> PayrollRun.builder()
                        .payPeriodMonth(month).payPeriodYear(year)
                        .status("IN_PROGRESS")
                        .totalEmployees(activeEmployees.size())
                        .processedCount(0).pendingCount(activeEmployees.size())
                        .emailSuccessCount(0).emailFailedCount(0)
                        .build());
        run = payrollRunRepository.save(run);

        int processed = 0, emailSuccess = 0, emailFailed = 0;

        for (Employee employee : activeEmployees) {
            try {
                Payslip payslip = generatePayslipForEmployee(employee, month, year);
                boolean sent = emailService.sendPayslipEmail(payslip);
                payslip.setStatus(sent ? PayslipStatus.EMAIL_SENT : PayslipStatus.EMAIL_FAILED);
                payslipRepository.save(payslip);

                processed++;
                if (sent) emailSuccess++; else emailFailed++;
            } catch (Exception ex) {
                log.error("Failed to process payroll for employee {}: {}", employee.getId(), ex.getMessage());
                emailFailed++;
            }
        }

        run.setProcessedCount(processed);
        run.setPendingCount(Math.max(0, activeEmployees.size() - processed));
        run.setEmailSuccessCount(emailSuccess);
        run.setEmailFailedCount(emailFailed);
        run.setStatus(emailFailed == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
        run.setCompletedAt(java.time.LocalDateTime.now());

        return payrollRunRepository.save(run);
    }

    /** Generates (and stores) a single employee's payslip for a period, without sending email. */
    @Transactional
    public Payslip generatePayslipForEmployee(Employee employee, int month, int year) {
        Optional<Payslip> existing = payslipRepository
                .findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(employee.getId(), month, year);
        if (existing.isPresent()) {
            throw new BadRequestException("Payslip already generated for this employee/period");
        }

        EmployeeSalaryHistory currentStructure = salaryHistoryRepository
                .findByEmployeeIdAndEffectiveToIsNull(employee.getId())
                .orElseThrow(() -> new BadRequestException(
                        "No salary structure assigned to " + employee.getEmployeeCode()));

        BigDecimal gross = currentStructure.getGrossSalary();
        BigDecimal pfDeduction = gross.multiply(currentStructure.getPfPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal esiDeduction = gross.multiply(currentStructure.getEsiPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalDeductions = pfDeduction.add(esiDeduction)
                .add(currentStructure.getProfessionalTax())
                .add(currentStructure.getOtherDeductions());
        BigDecimal netSalary = gross.subtract(totalDeductions);

        // ---- Leave summary for the payslip ----
        YearMonth period = YearMonth.of(year, month);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();

        BigDecimal[] leaveUsage = computeLeaveUsageForPeriod(employee.getId(), periodStart, periodEnd);
        BigDecimal clUsedInPeriod = leaveUsage[0];
        BigDecimal slUsedInPeriod = leaveUsage[1];
        BigDecimal elUsedInPeriod = leaveUsage[2];

        BigDecimal clRemaining = getRemainingBalance(employee.getId(), "CL", year);
        BigDecimal slRemaining = getRemainingBalance(employee.getId(), "SL", year);
        BigDecimal elRemaining = getRemainingBalance(employee.getId(), "EL", year);

        Payslip payslip = Payslip.builder()
                .employee(employee)
                .payPeriodMonth(month)
                .payPeriodYear(year)
                .basicSalary(currentStructure.getBasicSalary())
                .hra(currentStructure.getHra())
                .specialAllowance(currentStructure.getSpecialAllowance())
                .grossSalary(gross)
                .pfDeduction(pfDeduction)
                .esiDeduction(esiDeduction)
                .professionalTax(currentStructure.getProfessionalTax())
                .otherDeductions(currentStructure.getOtherDeductions())
                .totalDeductions(totalDeductions)
                .netSalary(netSalary)
                .clUsed(clUsedInPeriod)
                .clRemaining(clRemaining)
                .slUsed(slUsedInPeriod)
                .slRemaining(slRemaining)
                .elUsed(elUsedInPeriod)
                .elRemaining(elRemaining)
                .status(PayslipStatus.GENERATED)
                .build();

        payslip = payslipRepository.save(payslip);

        try {
            String pdfPath = pdfGeneratorService.generatePayslipPdf(payslip);
            payslip.setPdfPath(pdfPath);
            payslip = payslipRepository.save(payslip);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate PDF for payslip " + payslip.getId(), ex);
        }

        return payslip;
    }

    private BigDecimal[] computeLeaveUsageForPeriod(Long employeeId, LocalDate start, LocalDate end) {
        List<LeaveRequest> approved = leaveRequestRepository
                .findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);

        BigDecimal cl = BigDecimal.ZERO, sl = BigDecimal.ZERO, el = BigDecimal.ZERO;
        for (LeaveRequest r : approved) {
            if (!r.getStartDate().isAfter(end) && !r.getEndDate().isBefore(start)) {
                switch (r.getLeaveType().getCode()) {
                    case "CL" -> cl = cl.add(r.getDaysRequested());
                    case "SL" -> sl = sl.add(r.getDaysRequested());
                    case "EL" -> el = el.add(r.getDaysRequested());
                }
            }
        }
        return new BigDecimal[]{cl, sl, el};
    }

    private BigDecimal getRemainingBalance(Long employeeId, String typeCode, int year) {
        LeaveType type = leaveTypeRepository.findByCode(typeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + typeCode));
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, type.getId(), year)
                .map(LeaveBalance::getRemaining)
                .orElse(type.getAnnualQuota());
    }

    public List<PayslipResponse> getEmployeePayslips(Long employeeId) {
        return payslipRepository.findByEmployeeIdOrderByPayPeriodYearDescPayPeriodMonthDesc(employeeId)
                .stream().map(this::toResponse).toList();
    }

    public PayslipResponse getPayslip(Long payslipId) {
        return toResponse(payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + payslipId)));
    }

    public Payslip getPayslipEntity(Long payslipId) {
        return payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found: " + payslipId));
    }

    /** Re-attempts email delivery for a payslip that previously failed. */
    @Transactional
    public boolean retryEmail(Long payslipId) {
        Payslip payslip = getPayslipEntity(payslipId);
        boolean sent = emailService.sendPayslipEmail(payslip);
        payslip.setStatus(sent ? PayslipStatus.EMAIL_SENT : PayslipStatus.EMAIL_FAILED);
        payslipRepository.save(payslip);
        return sent;
    }

    private PayslipResponse toResponse(Payslip p) {
        return PayslipResponse.builder()
                .id(p.getId())
                .employeeCode(p.getEmployee().getEmployeeCode())
                .employeeName(p.getEmployee().getFullName())
                .department(p.getEmployee().getDepartment() != null ? p.getEmployee().getDepartment().getName() : null)
                .designation(p.getEmployee().getDesignation())
                .payPeriodMonth(p.getPayPeriodMonth())
                .payPeriodYear(p.getPayPeriodYear())
                .basicSalary(p.getBasicSalary())
                .hra(p.getHra())
                .specialAllowance(p.getSpecialAllowance())
                .grossSalary(p.getGrossSalary())
                .pfDeduction(p.getPfDeduction())
                .esiDeduction(p.getEsiDeduction())
                .professionalTax(p.getProfessionalTax())
                .otherDeductions(p.getOtherDeductions())
                .totalDeductions(p.getTotalDeductions())
                .netSalary(p.getNetSalary())
                .clUsed(p.getClUsed()).clRemaining(p.getClRemaining())
                .slUsed(p.getSlUsed()).slRemaining(p.getSlRemaining())
                .elUsed(p.getElUsed()).elRemaining(p.getElRemaining())
                .status(p.getStatus().name())
                .pdfDownloadUrl("/api/payroll/payslips/" + p.getId() + "/download")
                .build();
    }
}
