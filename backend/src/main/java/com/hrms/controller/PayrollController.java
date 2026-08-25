package com.hrms.controller;

import com.hrms.dto.request.RunPayrollRequest;
import com.hrms.dto.response.PayslipResponse;
import com.hrms.entity.Employee;
import com.hrms.entity.PayrollRun;
import com.hrms.entity.Payslip;
import com.hrms.service.PayslipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayslipService payslipService;

    /** HR/ADMIN: kicks off payroll generation + auto-email for an entire period, on demand. */
    @PostMapping("/run")
    public ResponseEntity<PayrollRun> runPayroll(@Valid @RequestBody RunPayrollRequest request) {
        return ResponseEntity.ok(payslipService.runMonthlyPayroll(request.getMonth(), request.getYear()));
    }

    /** HR/ADMIN: retry sending a specific payslip's email if it previously failed. */
    @PostMapping("/payslips/{id}/retry-email")
    public ResponseEntity<Boolean> retryEmail(@PathVariable Long id) {
        return ResponseEntity.ok(payslipService.retryEmail(id));
    }

    /** Employee: view own payslip history. HR/ADMIN can pass any employeeId via /employee/{id}. */
    @GetMapping("/payslips/my")
    public ResponseEntity<List<PayslipResponse>> myPayslips(@AuthenticationPrincipal Employee principal) {
        return ResponseEntity.ok(payslipService.getEmployeePayslips(principal.getId()));
    }

    @GetMapping("/payslips/employee/{employeeId}")
    public ResponseEntity<List<PayslipResponse>> employeePayslips(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payslipService.getEmployeePayslips(employeeId));
    }

    @GetMapping("/payslips/{id}")
    public ResponseEntity<PayslipResponse> getPayslip(@PathVariable Long id) {
        return ResponseEntity.ok(payslipService.getPayslip(id));
    }

    /** Download the actual PDF. Employees can only fetch their own (checked below); HR/ADMIN can fetch any. */
    @GetMapping("/payslips/{id}/download")
    public ResponseEntity<Resource> downloadPdf(@AuthenticationPrincipal Employee principal, @PathVariable Long id) {
        Payslip payslip = payslipService.getPayslipEntity(id);

        boolean isOwner = payslip.getEmployee().getId().equals(principal.getId());
        boolean isHrOrAdmin = principal.getRole().name().equals("HR") || principal.getRole().name().equals("ADMIN");
        if (!isOwner && !isHrOrAdmin) {
            return ResponseEntity.status(403).build();
        }

        File file = new File(payslip.getPdfPath());
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
