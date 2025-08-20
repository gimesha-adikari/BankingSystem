package com.bankingsystem.core.features.kyc.application.impl;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.features.kyc.domain.repository.KycCaseRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class KycCaseServiceImpl implements KycCaseService {

    private final KycCaseRepository cases;
    private final KycUploadRepository uploads;

    public KycCaseServiceImpl(KycCaseRepository cases, KycUploadRepository uploads) {
        this.cases = cases;
        this.uploads = uploads;
    }

    @Override
    @Transactional
    public KycCase submit(UUID userId, String docFrontId, String docBackId, String selfieId, String addressId) {
        List<UUID> ids = Arrays.asList(
                UUID.fromString(docFrontId),
                UUID.fromString(docBackId),
                UUID.fromString(selfieId),
                UUID.fromString(addressId)
        );

        long cnt = uploads.countByIdInAndUploadedBy(ids, userId);
        if (cnt != ids.size()) {
            throw new IllegalArgumentException("Upload IDs invalid or not owned by user");
        }

        KycCase existingPending = cases.findByUserIdAndStatus(userId, KycStatus.PENDING).orElse(null);
        KycCase c = (existingPending != null) ? existingPending : new KycCase();
        c.setUserId(userId);
        c.setDocFrontId(docFrontId);
        c.setDocBackId(docBackId);
        c.setSelfieId(selfieId);
        c.setAddressId(addressId);
        c.setStatus(KycStatus.PENDING);
        return cases.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KycCase> getMyLatest(UUID userId) {
        return cases.findTopByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KycCase> listByStatus(KycStatus status, Pageable pageable) {
        return cases.findAllByStatus(status, pageable);
    }

    @Override
    @Transactional
    public KycCase decide(String caseId, KycStatus target, String reason) {
        if (target != KycStatus.APPROVED && target != KycStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        }
        KycCase c = cases.findById(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        if (c.getStatus() == KycStatus.APPROVED || c.getStatus() == KycStatus.REJECTED) {
            return c;
        }
        c.setStatus(target);
        c.setDecisionReason(reason);
        return cases.save(c);
    }
}
