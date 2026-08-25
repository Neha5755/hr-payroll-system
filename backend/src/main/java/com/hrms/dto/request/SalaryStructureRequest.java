package com.hrms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalaryStructureRequest {
    @NotBlank private String name;
    @NotNull @PositiveOrZero private BigDecimal basicSalary;
    @NotNull @PositiveOrZero private BigDecimal hra;
    @NotNull @PositiveOrZero private BigDecimal specialAllowance;
    private BigDecimal pfPercent = BigDecimal.valueOf(12.00);
    private BigDecimal esiPercent = BigDecimal.valueOf(0.75);
    private BigDecimal professionalTax = BigDecimal.valueOf(200.00);
    private BigDecimal otherDeductions = BigDecimal.ZERO;
}
