package com.bankingsystem.core.config;

import com.bankingsystem.core.entity.Role;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.repository.RoleRepository;
import com.bankingsystem.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Order(2)
@Component
@RequiredArgsConstructor
public class DefaultUsersInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DefaultUsersInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final AppProperties appProperties;

    @Override
    public void run(String... args) throws Exception {
        createDefaultUserIfNotExists("ADMIN",
                appProperties.getDefaultAdmin().getUsername(),
                appProperties.getDefaultAdmin().getEmail(),
                appProperties.getDefaultAdmin().getPassword());

        createDefaultUserIfNotExists("CUSTOMER",
                appProperties.getDefaultCustomer().getUsername(),
                appProperties.getDefaultCustomer().getEmail(),
                appProperties.getDefaultCustomer().getPassword());

        createDefaultUserIfNotExists("TELLER",
                appProperties.getDefaultTeller().getUsername(),
                appProperties.getDefaultTeller().getEmail(),
                appProperties.getDefaultTeller().getPassword());

        createDefaultUserIfNotExists("MANAGER",
                appProperties.getDefaultManager().getUsername(),
                appProperties.getDefaultManager().getEmail(),
                appProperties.getDefaultManager().getPassword());
    }

    private void createDefaultUserIfNotExists(String roleName, String username, String email, String rawPassword) {
        if (!userRepository.existsByUsernameIgnoreCase(username)) {
            Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                    .orElseThrow(() -> new RuntimeException(roleName + " role not found"));

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now());

            userRepository.save(user);
            logger.info("Default {} user created successfully: {}", roleName, username);
        } else {
            logger.info("{} user already exists: {}", roleName, username);
        }
    }
}
