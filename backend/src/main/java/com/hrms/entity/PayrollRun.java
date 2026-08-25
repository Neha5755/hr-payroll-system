package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_runs", uniqueConstraints = @UniqueConstraint(columnNames = {"pay_period_month", "pay_period_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pay_period_month", nullable = false)
    private Integer payPeriodMonth;

    @Column(name = "pay_period_year", nullable = false)
    private Integer payPeriodYear;

    @Column(name = "total_employees") private Integer totalEmployees;
    @Column(name = "processed_count") private Integer processedCount;
    @Column(name = "pending_count") private Integer pendingCount;
    @Column(name = "email_success_count") private Integer emailSuccessCount;
    @Column(name = "email_failed_count") private Integer emailFailedCount;

    @Column(nullable = false)
    private String status;   // IN_PROGRESS | COMPLETED | COMPLETED_WITH_ERRORS

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() { startedAt = LocalDateTime.now(); }
}
