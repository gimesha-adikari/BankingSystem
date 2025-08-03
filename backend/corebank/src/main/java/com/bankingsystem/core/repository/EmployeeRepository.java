package com.bankingsystem.core.repository;

import com.bankingsystem.core.entity.Employee;
import com.bankingsystem.core.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUser_UserId(UUID userId);
    List<Employee> findByRole(Role role);
}
