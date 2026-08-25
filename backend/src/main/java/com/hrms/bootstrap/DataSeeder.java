package com.hrms.bootstrap;

import com.hrms.entity.Employee;
import com.hrms.entity.Role;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * On first boot, if there are NO employees in the database yet, creates one default
 * ADMIN account so you have a way to log in and create everyone else through the API/UI.
 *
 * Default login (CHANGE THIS IMMEDIATELY in any real deployment):
 *   email:    admin@company.com
 *   password: Admin@123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (employeeRepository.count() > 0) {
            return; // already has data, don't touch it
        }

        Employee admin = Employee.builder()
                .employeeCode("EMP1000")
                .firstName("System")
                .lastName("Admin")
                .email("admin@company.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .designation("Administrator")
                .dateOfJoining(LocalDate.now())
                .active(true)
                .build();

        employeeRepository.save(admin);

        log.warn("=================================================================");
        log.warn("No employees found — seeded a default ADMIN account:");
        log.warn("  email:    admin@company.com");
        log.warn("  password: Admin@123");
        log.warn("CHANGE THIS PASSWORD (or delete/replace this user) before real use.");
        log.warn("=================================================================");
    }
}
