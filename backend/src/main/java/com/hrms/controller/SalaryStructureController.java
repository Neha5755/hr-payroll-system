package com.hrms.controller;

import com.hrms.dto.request.AssignSalaryStructureRequest;
import com.hrms.dto.request.SalaryStructureRequest;
import com.hrms.dto.response.EmployeeSalaryHistoryResponse;
import com.hrms.dto.response.SalaryStructureResponse;
import com.hrms.entity.Employee;
import com.hrms.service.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-structures")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final SalaryService salaryService;

    // ---- CRUD on structure templates (HR/ADMIN only, enforced in SecurityConfig) ----

    @PostMapping
    public ResponseEntity<SalaryStructureResponse> create(@Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(salaryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SalaryStructureResponse>> getAll() {
        return ResponseEntity.ok(salaryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(salaryService.getOne(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> update(@PathVariable Long id, @Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(salaryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salaryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Assignment + history ----

    @PostMapping("/assign")
    public ResponseEntity<Void> assign(@AuthenticationPrincipal Employee principal,
                                        @Valid @RequestBody AssignSalaryStructureRequest request) {
        salaryService.assignToEmployee(request, principal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/employee/{employeeId}/current")
    public ResponseEntity<EmployeeSalaryHistoryResponse> currentForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryService.getCurrentStructureForEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/history")
    public ResponseEntity<List<EmployeeSalaryHistoryResponse>> historyForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryService.getHistoryForEmployee(employeeId));
    }
}
