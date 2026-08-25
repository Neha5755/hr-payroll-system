package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "leave_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;           // CL, SL, EL

    @Column(nullable = false)
    private String name;

    @Column(name = "annual_quota", nullable = false)
    private BigDecimal annualQuota;
}
