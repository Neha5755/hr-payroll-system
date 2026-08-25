package com.hrms.repository;

import com.hrms.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByPayPeriodMonthAndPayPeriodYear(Integer month, Integer year);
}
