
package com.bankingsystem.core.config;

import com.bankingsystem.core.entity.Role;
import com.bankingsystem.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("EMPLOYEE");
        createRoleIfNotExists("MANAGER");
        createRoleIfNotExists("TELLER");
        createRoleIfNotExists("CUSTOMER");
    }

    private void createRoleIfNotExists(String roleName) {
        roleRepository.findByRoleName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(roleName);
            return roleRepository.save(role);
        });
    }
}
