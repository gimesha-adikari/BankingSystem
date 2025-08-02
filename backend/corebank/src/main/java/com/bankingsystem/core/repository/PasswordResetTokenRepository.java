package com.bankingsystem.core.repository;

import com.bankingsystem.core.entity.PasswordResetToken;
import com.bankingsystem.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
