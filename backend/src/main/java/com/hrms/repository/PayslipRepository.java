package com.hrms.repository;

import com.hrms.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployeeIdOrderByPayPeriodYearDescPayPeriodMonthDesc(Long employeeId);
    Optional<Payslip> findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(Long employeeId, Integer month, Integer year);
    List<Payslip> findByPayPeriodMonthAndPayPeriodYear(Integer month, Integer year);
    long countByPayPeriodMonthAndPayPeriodYear(Integer month, Integer year);
    long countByPayPeriodMonthAndPayPeriodYearAndStatus(Integer month, Integer year, com.hrms.entity.PayslipStatus status);
}
