package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "leave_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "leave_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private BigDecimal allocated;

    @Column(nullable = false)
    private BigDecimal used;

    @Column(nullable = false)
    private BigDecimal remaining;

    public void deduct(BigDecimal days) {
        this.used = this.used.add(days);
        this.remaining = this.remaining.subtract(days);
    }

    public void restore(BigDecimal days) {
        this.used = this.used.subtract(days);
        this.remaining = this.remaining.add(days);
    }
}
