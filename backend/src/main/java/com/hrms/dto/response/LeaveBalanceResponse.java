package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveBalanceResponse {
    private String leaveTypeCode;
    private String leaveTypeName;
    private BigDecimal allocated;
    private BigDecimal used;
    private BigDecimal remaining;
}
