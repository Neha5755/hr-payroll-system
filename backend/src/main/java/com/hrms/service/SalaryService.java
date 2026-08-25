package com.hrms.service;

import com.hrms.dto.request.AssignSalaryStructureRequest;
import com.hrms.dto.request.SalaryStructureRequest;
import com.hrms.dto.response.EmployeeSalaryHistoryResponse;
import com.hrms.dto.response.SalaryStructureResponse;
import com.hrms.entity.*;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeSalaryStructureRepository employeeSalaryStructureRepository;
    private final EmployeeSalaryHistoryRepository employeeSalaryHistoryRepository;
    private final EmployeeRepository employeeRepository;

    // ---------- CRUD on salary structure templates ----------

    @Transactional
    public SalaryStructureResponse create(SalaryStructureRequest req) {
        SalaryStructure structure = SalaryStructure.builder()
                .name(req.getName())
                .basicSalary(req.getBasicSalary())
                .hra(req.getHra())
                .specialAllowance(req.getSpecialAllowance())
                .pfPercent(req.getPfPercent())
                .esiPercent(req.getEsiPercent())
                .professionalTax(req.getProfessionalTax())
                .otherDeductions(req.getOtherDeductions())
                .active(true)
                .build();
        return toResponse(salaryStructureRepository.save(structure));
    }

    @Transactional
    public SalaryStructureResponse update(Long id, SalaryStructureRequest req) {
        SalaryStructure structure = salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id));

        structure.setName(req.getName());
        structure.setBasicSalary(req.getBasicSalary());
        structure.setHra(req.getHra());
        structure.setSpecialAllowance(req.getSpecialAllowance());
        structure.setPfPercent(req.getPfPercent());
        structure.setEsiPercent(req.getEsiPercent());
        structure.setProfessionalTax(req.getProfessionalTax());
        structure.setOtherDeductions(req.getOtherDeductions());

        return toResponse(salaryStructureRepository.save(structure));
    }

    @Transactional
    public void delete(Long id) {
        SalaryStructure structure = salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id));
        // Soft-delete: keep historical integrity for employees who were once on this structure
        structure.setActive(false);
        salaryStructureRepository.save(structure);
    }

    public List<SalaryStructureResponse> getAll() {
        return salaryStructureRepository.findAll().stream().map(this::toResponse).toList();
    }

    public SalaryStructureResponse getOne(Long id) {
        return toResponse(salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id)));
    }

    // ---------- Assignment + history ----------

    @Transactional
    public void assignToEmployee(AssignSalaryStructureRequest req, Long changedByEmployeeId) {
        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        SalaryStructure structure = salaryStructureRepository.findById(req.getSalaryStructureId())
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        if (!structure.isActive()) {
            throw new BadRequestException("Cannot assign an inactive salary structure");
        }

        Employee changedBy = changedByEmployeeId != null ? employeeRepository.findById(changedByEmployeeId).orElse(null) : null;

        // Close out the previous "current" history row, if any
        employeeSalaryHistoryRepository.findByEmployeeIdAndEffectiveToIsNull(employee.getId())
                .ifPresent(prev -> {
                    prev.setEffectiveTo(req.getEffectiveFrom().minusDays(1));
                    employeeSalaryHistoryRepository.save(prev);
                });

        // Record the new history snapshot (immutable point-in-time record)
        EmployeeSalaryHistory history = EmployeeSalaryHistory.builder()
                .employee(employee)
                .salaryStructure(structure)
                .basicSalary(structure.getBasicSalary())
                .hra(structure.getHra())
                .specialAllowance(structure.getSpecialAllowance())
                .grossSalary(structure.getGrossSalary())
                .pfPercent(structure.getPfPercent())
                .esiPercent(structure.getEsiPercent())
                .professionalTax(structure.getProfessionalTax())
                .otherDeductions(structure.getOtherDeductions())
                .effectiveFrom(req.getEffectiveFrom())
                .effectiveTo(null)
                .changedBy(changedBy)
                .build();
        employeeSalaryHistoryRepository.save(history);

        // Upsert the "current assignment" pointer table
        EmployeeSalaryStructure assignment = employeeSalaryStructureRepository.findByEmployeeId(employee.getId())
                .orElse(EmployeeSalaryStructure.builder().employee(employee).build());
        assignment.setSalaryStructure(structure);
        assignment.setEffectiveFrom(req.getEffectiveFrom());
        employeeSalaryStructureRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public EmployeeSalaryHistoryResponse getCurrentStructureForEmployee(Long employeeId) {
        EmployeeSalaryHistory history = employeeSalaryHistoryRepository.findByEmployeeIdAndEffectiveToIsNull(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No salary structure has been assigned to employee " + employeeId));
        return toHistoryResponse(history);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSalaryHistoryResponse> getHistoryForEmployee(Long employeeId) {
        return employeeSalaryHistoryRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private EmployeeSalaryHistoryResponse toHistoryResponse(EmployeeSalaryHistory h) {
        Employee employee = h.getEmployee();
        Employee changedBy = h.getChangedBy();
        return EmployeeSalaryHistoryResponse.builder()
                .id(h.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeFullName(employee.getFullName())
                .salaryStructureName(h.getSalaryStructure().getName())
                .basicSalary(h.getBasicSalary())
                .hra(h.getHra())
                .specialAllowance(h.getSpecialAllowance())
                .grossSalary(h.getGrossSalary())
                .pfPercent(h.getPfPercent())
                .esiPercent(h.getEsiPercent())
                .professionalTax(h.getProfessionalTax())
                .otherDeductions(h.getOtherDeductions())
                .effectiveFrom(h.getEffectiveFrom())
                .effectiveTo(h.getEffectiveTo())
                .changedByName(changedBy != null ? changedBy.getFullName() : null)
                .build();
    }
    private SalaryStructureResponse toResponse(SalaryStructure s) {
        return SalaryStructureResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .basicSalary(s.getBasicSalary())
                .hra(s.getHra())
                .specialAllowance(s.getSpecialAllowance())
                .grossSalary(s.getGrossSalary())
                .pfPercent(s.getPfPercent())
                .esiPercent(s.getEsiPercent())
                .professionalTax(s.getProfessionalTax())
                .otherDeductions(s.getOtherDeductions())
                .active(s.isActive())
                .build();
    }
}
