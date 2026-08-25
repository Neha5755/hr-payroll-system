package com.hrms.repository;

import com.hrms.entity.EmailLog;
import com.hrms.entity.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByPayslipId(Long payslipId);
    long countByStatus(EmailStatus status);
}
