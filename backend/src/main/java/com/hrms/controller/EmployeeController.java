package com.hrms.controller;

import com.hrms.dto.request.EmployeeCreateRequest;
import com.hrms.dto.response.EmployeeResponse;
import com.hrms.entity.Employee;
import com.hrms.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // NOTE: create/update/list/deactivate require HR/ADMIN role (enforced in SecurityConfig)

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.ok(employeeService.createEmployee(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmployeeResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivateEmployee(id));
    }

    /** The logged-in employee's own profile (any authenticated role). */
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyProfile(@AuthenticationPrincipal Employee principal) {
        return ResponseEntity.ok(employeeService.getEmployee(principal.getId()));
    }
}
