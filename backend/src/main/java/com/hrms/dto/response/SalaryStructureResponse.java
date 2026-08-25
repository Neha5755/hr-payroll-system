package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SalaryStructureResponse {
    private Long id;
    private String name;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal specialAllowance;
    private BigDecimal grossSalary;
    private BigDecimal pfPercent;
    private BigDecimal esiPercent;
    private BigDecimal professionalTax;
    private BigDecimal otherDeductions;
    private boolean active;
}
