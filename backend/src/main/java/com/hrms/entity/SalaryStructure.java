package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_structures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "basic_salary", nullable = false)
    private BigDecimal basicSalary;

    @Column(nullable = false)
    private BigDecimal hra;

    @Column(name = "special_allowance", nullable = false)
    private BigDecimal specialAllowance;

    @Column(name = "pf_percent", nullable = false)
    private BigDecimal pfPercent;

    @Column(name = "esi_percent", nullable = false)
    private BigDecimal esiPercent;

    @Column(name = "professional_tax", nullable = false)
    private BigDecimal professionalTax;

    @Column(name = "other_deductions", nullable = false)
    private BigDecimal otherDeductions;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public BigDecimal getGrossSalary() {
        return basicSalary.add(hra).add(specialAllowance);
    }
}
