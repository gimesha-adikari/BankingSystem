package com.bankingsystem.core.features.kyc.application;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface KycCaseService {
    KycCase submit(UUID userId, String docFrontId, String docBackId, String selfieId, String addressId);
    Optional<KycCase> getMyLatest(UUID userId);
    Page<KycCase> listByStatus(KycStatus status, Pageable pageable);
    KycCase decide(String caseId, KycStatus target, String reason, UUID reviewerId);
    KycCase markStatus(String caseId, KycStatus status, String note);
}
