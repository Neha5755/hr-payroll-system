package com.hrms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AssignSalaryStructureRequest {
    @NotNull private Long employeeId;
    @NotNull private Long salaryStructureId;
    @NotNull private LocalDate effectiveFrom;
}
