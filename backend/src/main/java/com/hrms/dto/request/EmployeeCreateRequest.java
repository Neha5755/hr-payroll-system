package com.hrms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeCreateRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") private String password;
    @NotBlank private String role;          // EMPLOYEE | HR | ADMIN
    private Long departmentId;
    private String designation;
    @NotNull private LocalDate dateOfJoining;
}
