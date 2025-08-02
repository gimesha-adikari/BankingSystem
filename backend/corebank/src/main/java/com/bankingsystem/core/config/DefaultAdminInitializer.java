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
public class DefaultAdminInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final AppProperties appProperties;


    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername(appProperties.getDefaultAdmin().getUsername())) {
            Role adminRole = roleRepository.findByRoleName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = new User();
            admin.setUsername(appProperties.getDefaultAdmin().getUsername());
            admin.setEmail(appProperties.getDefaultAdmin().getEmail());
            admin.setPasswordHash(passwordEncoder.encode(appProperties.getDefaultAdmin().getPassword()));
            admin.setRole(adminRole);
            admin.setIsActive(true);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);
            logger.info("Default admin user created successfully");
        } else {
            logger.info("Admin user already exists");
        }
    }
}
