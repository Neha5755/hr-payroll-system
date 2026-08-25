package com.hrms.service;

import com.hrms.dto.request.LeaveRequestCreateRequest;
import com.hrms.dto.response.LeaveBalanceResponse;
import com.hrms.dto.response.LeaveRequestResponse;
import com.hrms.entity.*;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.InsufficientLeaveBalanceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Handles leave requests and is the single place where leave balances are mutated.
 * Balances are only deducted the moment a request is APPROVED (never on submission),
 * which is what "Leave–Payroll Integration" in the spec requires.
 */
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public LeaveRequestResponse applyForLeave(Long employeeId, LeaveRequestCreateRequest req) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        LeaveType leaveType = leaveTypeRepository.findByCode(req.getLeaveTypeCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Unknown leave type: " + req.getLeaveTypeCode()));

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        BigDecimal days = BigDecimal.valueOf(
                ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1);

        // Validate there IS enough balance before allowing submission (soft check;
        // hard deduction only happens on approval so pending requests don't block others).
        LeaveBalance balance = getOrCreateBalance(employee, leaveType, Year.now().getValue());
        if (balance.getRemaining().compareTo(days) < 0) {
            throw new InsufficientLeaveBalanceException(
                    "Insufficient " + leaveType.getCode() + " balance. Remaining: " + balance.getRemaining()
                            + ", Requested: " + days);
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .daysRequested(days)
                .reason(req.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        return toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Transactional
    public LeaveRequestResponse approveLeave(Long leaveRequestId, Long approverId) {
        LeaveRequest request = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        LeaveBalance balance = getOrCreateBalance(
                request.getEmployee(), request.getLeaveType(), request.getStartDate().getYear());

        if (balance.getRemaining().compareTo(request.getDaysRequested()) < 0) {
            throw new InsufficientLeaveBalanceException(
                    "Cannot approve: insufficient " + request.getLeaveType().getCode() + " balance remaining");
        }

        // ---- automatic deduction on approval ----
        balance.deduct(request.getDaysRequested());
        leaveBalanceRepository.save(balance);

        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedAt(java.time.LocalDateTime.now());
        Employee approver = employeeRepository.findById(approverId).orElse(null);
        request.setApprovedBy(approver);

        return toResponse(leaveRequestRepository.save(request));
    }

    @Transactional
    public LeaveRequestResponse rejectLeave(Long leaveRequestId, Long approverId) {
        LeaveRequest request = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be rejected");
        }

        request.setStatus(LeaveStatus.REJECTED);
        request.setApprovedAt(java.time.LocalDateTime.now());
        Employee approver = employeeRepository.findById(approverId).orElse(null);
        request.setApprovedBy(approver);

        return toResponse(leaveRequestRepository.save(request));
    }

    @Transactional
    public LeaveRequestResponse cancelLeave(Long leaveRequestId, Long requesterId) {
        LeaveRequest request = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (!request.getEmployee().getId().equals(requesterId)) {
            throw new BadRequestException("You can only cancel your own leave requests");
        }

        // If it was already approved, restore the balance that was deducted
        if (request.getStatus() == LeaveStatus.APPROVED) {
            LeaveBalance balance = getOrCreateBalance(
                    request.getEmployee(), request.getLeaveType(), request.getStartDate().getYear());
            balance.restore(request.getDaysRequested());
            leaveBalanceRepository.save(balance);
        }

        request.setStatus(LeaveStatus.CANCELLED);
        return toResponse(leaveRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingRequests() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getEmployeeHistory(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalances(Long employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .map(b -> LeaveBalanceResponse.builder()
                        .leaveTypeCode(b.getLeaveType().getCode())
                        .leaveTypeName(b.getLeaveType().getName())
                        .allocated(b.getAllocated())
                        .used(b.getUsed())
                        .remaining(b.getRemaining())
                        .build())
                .toList();
    }

    /**
     * Maps the JPA entity to a flat response DTO. This must run while the
     * originating Hibernate session is still open (i.e. inside a @Transactional
     * method) so that the lazy `employee` association can be initialized safely,
     * and so entities/proxies (and sensitive fields like the password hash) are
     * never serialized directly by Jackson.
     */
    private LeaveRequestResponse toResponse(LeaveRequest r) {
        Employee employee = r.getEmployee();
        Employee approver = r.getApprovedBy();
        return LeaveRequestResponse.builder()
                .id(r.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeFullName(employee.getFullName())
                .leaveTypeCode(r.getLeaveType().getCode())
                .leaveTypeName(r.getLeaveType().getName())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .daysRequested(r.getDaysRequested())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .approvedByName(approver != null ? approver.getFullName() : null)
                .approvedAt(r.getApprovedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private LeaveBalance getOrCreateBalance(Employee employee, LeaveType type, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
                .orElseGet(() -> leaveBalanceRepository.save(LeaveBalance.builder()
                        .employee(employee)
                        .leaveType(type)
                        .year(year)
                        .allocated(type.getAnnualQuota())
                        .used(BigDecimal.ZERO)
                        .remaining(type.getAnnualQuota())
                        .build()));
    }
}
