package com.hrms.service;

import com.hrms.dto.response.DashboardResponse;
import com.hrms.entity.EmailStatus;
import com.hrms.entity.LeaveStatus;
import com.hrms.entity.PayslipStatus;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmailLogRepository emailLogRepository;

    public DashboardResponse getDashboard(Integer month, Integer year) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();

        long totalActiveEmployees = employeeRepository.countByActiveTrue();
        long processedThisPeriod = payslipRepository.countByPayPeriodMonthAndPayPeriodYear(m, y);
        long pendingPayroll = Math.max(0, totalActiveEmployees - processedThisPeriod);
        long emailSuccess = payslipRepository.countByPayPeriodMonthAndPayPeriodYearAndStatus(m, y, PayslipStatus.EMAIL_SENT);
        long emailFailed = payslipRepository.countByPayPeriodMonthAndPayPeriodYearAndStatus(m, y, PayslipStatus.EMAIL_FAILED);
        long pendingLeaveApprovals = leaveRequestRepository.findByStatus(LeaveStatus.PENDING).size();

        List<Map<String, Object>> leaveSummary = buildLeaveSummary(y);

        return DashboardResponse.builder()
                .totalEmployees((int) totalActiveEmployees)
                .totalEmployeesProcessed((int) processedThisPeriod)
                .pendingPayroll((int) pendingPayroll)
                .payslipsSentSuccessfully((int) emailSuccess)
                .failedDeliveries((int) emailFailed)
                .pendingLeaveApprovals((int) pendingLeaveApprovals)
                .currentPeriodMonth(m)
                .currentPeriodYear(y)
                .leaveBalanceSummary(leaveSummary)
                .build();
    }

    /** Aggregate CL/SL/EL allocated/used/remaining across the whole organization for the given year. */
    private List<Map<String, Object>> buildLeaveSummary(int year) {
        List<Map<String, Object>> result = new ArrayList<>();

        leaveTypeRepository.findAll().forEach(type -> {
            var balances = leaveBalanceRepository.findByYear(year).stream()
                    .filter(b -> b.getLeaveType().getId().equals(type.getId()))
                    .toList();

            double allocated = balances.stream().mapToDouble(b -> b.getAllocated().doubleValue()).sum();
            double used = balances.stream().mapToDouble(b -> b.getUsed().doubleValue()).sum();
            double remaining = balances.stream().mapToDouble(b -> b.getRemaining().doubleValue()).sum();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("leaveTypeCode", type.getCode());
            row.put("leaveTypeName", type.getName());
            row.put("totalAllocated", allocated);
            row.put("totalUsed", used);
            row.put("totalRemaining", remaining);
            result.add(row);
        });

        return result;
    }
}
