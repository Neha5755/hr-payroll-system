package com.hrms.service;

import com.hrms.dto.request.EmployeeCreateRequest;
import com.hrms.dto.response.EmployeeResponse;
import com.hrms.dto.response.LeaveBalanceResponse;
import com.hrms.entity.*;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest req) {
        if (employeeRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("An employee with this email already exists");
        }

        Department department = null;
        if (req.getDepartmentId() != null) {
            department = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        }

        Employee employee = Employee.builder()
                .employeeCode(generateEmployeeCode())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.valueOf(req.getRole().toUpperCase()))
                .department(department)
                .designation(req.getDesignation())
                .dateOfJoining(req.getDateOfJoining())
                .active(true)
                .build();

        employee = employeeRepository.save(employee);

        // Auto-allocate the current year's leave balances (CL 12 / SL 12 / EL 15)
        initializeLeaveBalancesForYear(employee, Year.now().getValue());

        return toResponse(employee);
    }

    private void initializeLeaveBalancesForYear(Employee employee, int year) {
        for (LeaveType type : leaveTypeRepository.findAll()) {
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
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

    private String generateEmployeeCode() {
        long count = employeeRepository.count() + 1;
        return String.format("EMP%04d", 1000 + count);
    }

    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        return toResponse(employee);
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmployeeResponse deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
        employee.setActive(false);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeCreateRequest req) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));

        employee.setFirstName(req.getFirstName());
        employee.setLastName(req.getLastName());
        employee.setDesignation(req.getDesignation());
        employee.setRole(Role.valueOf(req.getRole().toUpperCase()));
        if (req.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        }
        return toResponse(employeeRepository.save(employee));
    }

    public Employee getEntityOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private EmployeeResponse toResponse(Employee e) {
        int currentYear = Year.now().getValue();
        List<LeaveBalanceResponse> balances = leaveBalanceRepository
                .findByEmployeeIdAndYear(e.getId(), currentYear).stream()
                .map(b -> LeaveBalanceResponse.builder()
                        .leaveTypeCode(b.getLeaveType().getCode())
                        .leaveTypeName(b.getLeaveType().getName())
                        .allocated(b.getAllocated())
                        .used(b.getUsed())
                        .remaining(b.getRemaining())
                        .build())
                .toList();

        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .role(e.getRole().name())
                .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .designation(e.getDesignation())
                .dateOfJoining(e.getDateOfJoining())
                .active(e.isActive())
                .leaveBalances(balances)
                .build();
    }
}
