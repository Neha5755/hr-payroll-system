package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayslipResponse {
    private Long id;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String designation;
    private Integer payPeriodMonth;
    private Integer payPeriodYear;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal specialAllowance;
    private BigDecimal grossSalary;
    private BigDecimal pfDeduction;
    private BigDecimal esiDeduction;
    private BigDecimal professionalTax;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private BigDecimal clUsed;
    private BigDecimal clRemaining;
    private BigDecimal slUsed;
    private BigDecimal slRemaining;
    private BigDecimal elUsed;
    private BigDecimal elRemaining;
    private String status;
    private String pdfDownloadUrl;
}
