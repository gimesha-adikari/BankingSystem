package com.bankingsystem.core.features.auth.domain.repository;

import com.bankingsystem.core.features.auth.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByToken(String token);
}
