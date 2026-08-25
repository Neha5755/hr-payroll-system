package com.hrms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    @PrePersist
    void onCreate() {
        attemptedAt = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
    }
}
