package com.hrms.repository;

import com.hrms.entity.EmployeeSalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryHistoryRepository extends JpaRepository<EmployeeSalaryHistory, Long> {
    List<EmployeeSalaryHistory> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
    Optional<EmployeeSalaryHistory> findByEmployeeIdAndEffectiveToIsNull(Long employeeId);
}
