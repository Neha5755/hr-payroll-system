package com.hrms.controller;

import com.hrms.dto.request.LeaveRequestCreateRequest;
import com.hrms.dto.response.LeaveBalanceResponse;
import com.hrms.dto.response.LeaveRequestResponse;
import com.hrms.entity.Employee;
import com.hrms.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    /** Employee applies for leave (self). */
    @PostMapping("/apply")
    public ResponseEntity<LeaveRequestResponse> apply(@AuthenticationPrincipal Employee principal,
                                               @Valid @RequestBody LeaveRequestCreateRequest request) {
        return ResponseEntity.ok(leaveService.applyForLeave(principal.getId(), request));
    }

    /** Employee's own leave history. */
    @GetMapping("/my-history")
    public ResponseEntity<List<LeaveRequestResponse>> myHistory(@AuthenticationPrincipal Employee principal) {
        return ResponseEntity.ok(leaveService.getEmployeeHistory(principal.getId()));
    }

    /** Employee's own current-year balances (CL/SL/EL). */
    @GetMapping("/my-balance")
    public ResponseEntity<List<LeaveBalanceResponse>> myBalance(@AuthenticationPrincipal Employee principal) {
        return ResponseEntity.ok(leaveService.getBalances(principal.getId(), Year.now().getValue()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse> cancel(@AuthenticationPrincipal Employee principal, @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.cancelLeave(id, principal.getId()));
    }

    // ---- HR / ADMIN only (enforced in SecurityConfig) ----

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestResponse>> pending() {
        return ResponseEntity.ok(leaveService.getPendingRequests());
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<LeaveRequestResponse> approve(@AuthenticationPrincipal Employee principal, @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.approveLeave(id, principal.getId()));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<LeaveRequestResponse> reject(@AuthenticationPrincipal Employee principal, @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.rejectLeave(id, principal.getId()));
    }

    @GetMapping("/employee/{employeeId}/balance")
    public ResponseEntity<List<LeaveBalanceResponse>> employeeBalance(@PathVariable Long employeeId,
                                                                @RequestParam(required = false) Integer year) {
        int y = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(leaveService.getBalances(employeeId, y));
    }
}
