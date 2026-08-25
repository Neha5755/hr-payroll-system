package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payslips", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "pay_period_month", "pay_period_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "pay_period_month", nullable = false)
    private Integer payPeriodMonth;

    @Column(name = "pay_period_year", nullable = false)
    private Integer payPeriodYear;

    @Column(name = "basic_salary", nullable = false)
    private BigDecimal basicSalary;

    @Column(nullable = false)
    private BigDecimal hra;

    @Column(name = "special_allowance", nullable = false)
    private BigDecimal specialAllowance;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(name = "pf_deduction", nullable = false)
    private BigDecimal pfDeduction;

    @Column(name = "esi_deduction", nullable = false)
    private BigDecimal esiDeduction;

    @Column(name = "professional_tax", nullable = false)
    private BigDecimal professionalTax;

    @Column(name = "other_deductions", nullable = false)
    private BigDecimal otherDeductions;

    @Column(name = "total_deductions", nullable = false)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false)
    private BigDecimal netSalary;

    @Column(name = "cl_used") private BigDecimal clUsed;
    @Column(name = "cl_remaining") private BigDecimal clRemaining;
    @Column(name = "sl_used") private BigDecimal slUsed;
    @Column(name = "sl_remaining") private BigDecimal slRemaining;
    @Column(name = "el_used") private BigDecimal elUsed;
    @Column(name = "el_remaining") private BigDecimal elRemaining;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayslipStatus status;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    void onCreate() {
        generatedAt = LocalDateTime.now();
        if (status == null) status = PayslipStatus.GENERATED;
    }
}
