package com.bankingsystem.core.features.kyc.domain.repository;

import com.bankingsystem.core.features.kyc.domain.KycCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycCheckRepository extends JpaRepository<KycCheck, String> {
    List<KycCheck> findByCaseId(String caseId);
}
