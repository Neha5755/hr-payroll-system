package com.hrms.controller;

import com.hrms.dto.response.DashboardResponse;
import com.hrms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** HR/ADMIN only (enforced in SecurityConfig). Defaults to the current month/year. */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@RequestParam(required = false) Integer month,
                                                            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getDashboard(month, year));
    }
}
