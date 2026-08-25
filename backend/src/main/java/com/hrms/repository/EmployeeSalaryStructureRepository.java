package com.hrms.repository;

import com.hrms.entity.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeSalaryStructureRepository extends JpaRepository<EmployeeSalaryStructure, Long> {
    Optional<EmployeeSalaryStructure> findByEmployeeId(Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}
