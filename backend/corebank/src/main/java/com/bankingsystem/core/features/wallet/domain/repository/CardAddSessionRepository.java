package com.bankingsystem.core.features.wallet.domain.repository;

import com.bankingsystem.core.features.wallet.domain.entity.CardAddSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardAddSessionRepository extends JpaRepository<CardAddSession, String> {
}
