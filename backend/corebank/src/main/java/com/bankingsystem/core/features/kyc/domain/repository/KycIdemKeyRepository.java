package com.bankingsystem.core.features.kyc.domain.repository;

import com.bankingsystem.core.features.kyc.domain.KycIdemKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KycIdemKeyRepository extends JpaRepository<KycIdemKey, UUID> {
    Optional<KycIdemKey> findByUserIdAndIdemKey(UUID userId, String key);
}