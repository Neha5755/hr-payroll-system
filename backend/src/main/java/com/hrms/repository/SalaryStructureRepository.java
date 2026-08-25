package com.hrms.repository;

import com.hrms.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    List<SalaryStructure> findByActiveTrue();
}
