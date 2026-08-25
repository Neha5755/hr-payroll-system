package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponse {
    private int totalEmployees;
    private int totalEmployeesProcessed;
    private int pendingPayroll;
    private int payslipsSentSuccessfully;
    private int failedDeliveries;
    private int pendingLeaveApprovals;
    private Integer currentPeriodMonth;
    private Integer currentPeriodYear;
    private List<Map<String, Object>> leaveBalanceSummary; // aggregate CL/SL/EL usage across org
}
