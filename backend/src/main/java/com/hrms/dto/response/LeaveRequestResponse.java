package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveRequestResponse {
    private Long id;

    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;

    private String leaveTypeCode;
    private String leaveTypeName;

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal daysRequested;
    private String reason;
    private String status;

    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
