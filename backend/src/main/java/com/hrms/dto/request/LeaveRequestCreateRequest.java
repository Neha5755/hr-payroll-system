package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestCreateRequest {
    @NotBlank private String leaveTypeCode;   // CL | SL | EL
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String reason;
}
