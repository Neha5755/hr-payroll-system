package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeSalaryHistoryResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;
    private String salaryStructureName;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal specialAllowance;
    private BigDecimal grossSalary;
    private BigDecimal pfPercent;
    private BigDecimal esiPercent;
    private BigDecimal professionalTax;
    private BigDecimal otherDeductions;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String changedByName;
}
