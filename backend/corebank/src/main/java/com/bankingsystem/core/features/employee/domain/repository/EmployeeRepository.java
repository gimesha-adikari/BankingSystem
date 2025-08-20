package com.bankingsystem.core.features.employee.domain.repository;

import com.bankingsystem.core.features.employee.domain.Employee;
import com.bankingsystem.core.features.accesscontrol.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUser_UserId(UUID userId);
    List<Employee> findByRole(Role role);

    @Query("SELECT e FROM Employee e WHERE e.role.roleName = :roleName AND " +
            "(LOWER(e.firstName) LIKE %:search% OR LOWER(e.lastName) LIKE %:search% OR LOWER(e.email) LIKE %:search%)")
    List<Employee> searchByRoleAndNameOrEmail(@Param("roleName") String roleName, @Param("search") String search);
    List<Employee> findByRoleRoleNameIgnoreCase(String roleName);

}
