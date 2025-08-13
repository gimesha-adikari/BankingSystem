
package com.bankingsystem.core.config;

import com.bankingsystem.core.entity.Role;
import com.bankingsystem.core.enums.Roles;
import com.bankingsystem.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        createRoleIfNotExists(Roles.ADMIN.name());
        createRoleIfNotExists(Roles.EMPLOYEE.name());
        createRoleIfNotExists(Roles.MANAGER.name());
        createRoleIfNotExists(Roles.TELLER.name());
        createRoleIfNotExists(Roles.CUSTOMER.name());
    }

    private void createRoleIfNotExists(String roleName) {
        roleRepository.findByRoleNameIgnoreCase(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(roleName);
            return roleRepository.save(role);
        });
    }
}
