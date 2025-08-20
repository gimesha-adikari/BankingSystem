package com.bankingsystem.core.features.kyc.domain.repository;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KycCaseRepository extends JpaRepository<KycCase, String> {
    Optional<KycCase> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<KycCase> findByUserIdAndStatus(UUID userId, KycStatus status);
    Page<KycCase> findAllByStatus(KycStatus status, Pageable pageable);
}
