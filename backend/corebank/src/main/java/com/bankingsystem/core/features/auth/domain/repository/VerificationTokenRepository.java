package com.bankingsystem.core.features.auth.domain.repository;

import com.bankingsystem.core.features.auth.domain.VerificationToken;
import com.bankingsystem.core.features.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);
}
