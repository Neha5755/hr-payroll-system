package com.hrms.service;

import com.hrms.config.JwtUtil;
import com.hrms.dto.request.LoginRequest;
import com.hrms.dto.response.AuthResponse;
import com.hrms.entity.Employee;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        String token = jwtUtil.generateToken(employee, employee.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(employee.getEmail())
                .fullName(employee.getFullName())
                .role(employee.getRole().name())
                .employeeId(employee.getId())
                .build();
    }
}
