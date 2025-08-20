package com.bankingsystem.core.features.accesscontrol.domain.repository;

import com.bankingsystem.core.features.accesscontrol.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByRoleNameIgnoreCase(String roleName);
    boolean existsByRoleName(String roleName);
}
