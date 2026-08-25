package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** Points to the CURRENTLY active structure assignment for an employee. */
@Entity
@Table(name = "employee_salary_structure")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeSalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
}
