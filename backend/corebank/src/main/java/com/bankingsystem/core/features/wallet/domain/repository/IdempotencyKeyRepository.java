package com.bankingsystem.core.features.wallet.domain.repository;

import com.bankingsystem.core.features.wallet.domain.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> { }
