package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Immutable snapshot recorded every time a salary structure is assigned/changed for an employee. */
@Entity
@Table(name = "employee_salary_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeSalaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    @Column(name = "basic_salary", nullable = false)
    private BigDecimal basicSalary;

    @Column(nullable = false)
    private BigDecimal hra;

    @Column(name = "special_allowance", nullable = false)
    private BigDecimal specialAllowance;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(name = "pf_percent", nullable = false)
    private BigDecimal pfPercent;

    @Column(name = "esi_percent", nullable = false)
    private BigDecimal esiPercent;

    @Column(name = "professional_tax", nullable = false)
    private BigDecimal professionalTax;

    @Column(name = "other_deductions", nullable = false)
    private BigDecimal otherDeductions;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;      // null => currently active row

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private Employee changedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
