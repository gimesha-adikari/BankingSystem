package com.bankingsystem.core.features.kyc.application.impl;

import com.bankingsystem.core.features.kyc.application.KycCaseService;
import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.domain.KycUpload;
import com.bankingsystem.core.features.kyc.domain.repository.KycCaseRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        // Parse & basic ownership
        List<UUID> ids = Arrays.asList(
                UUID.fromString(docFrontId),
                UUID.fromString(docBackId),
                UUID.fromString(selfieId),
                UUID.fromString(addressId)
        );

        if (new HashSet<>(ids).size() != 4) {
            throw new IllegalArgumentException("Uploads must be four distinct files");
        }

        long owned = uploads.countByIdInAndUploadedBy(ids, userId);
        if (owned != ids.size()) {
            throw new IllegalArgumentException("Upload IDs invalid or not owned by user");
        }

        Map<UUID, KycUpload> byId = uploads.findByIdIn(ids).stream()
                .collect(Collectors.toMap(KycUpload::getId, u -> u));

        requireType(byId, UUID.fromString(docFrontId), "DOC_FRONT", "docFrontId");
        requireType(byId, UUID.fromString(docBackId), "DOC_BACK", "docBackId");
        requireType(byId, UUID.fromString(selfieId), "SELFIE", "selfieId");
        requireType(byId, UUID.fromString(addressId), "ADDRESS_PROOF", "addressId");

        List<KycStatus> active = Arrays.asList(
                KycStatus.PENDING, KycStatus.AUTO_REVIEW, KycStatus.UNDER_REVIEW, KycStatus.NEEDS_MORE_INFO
        );
        KycCase existingActive = cases.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(userId, active).orElse(null);

        KycCase c;
        if (existingActive == null) {
            c = new KycCase();
            c.setUserId(userId);
            c.setStatus(KycStatus.PENDING);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(c.getCreatedAt());
        } else if (existingActive.getStatus() == KycStatus.PENDING || existingActive.getStatus() == KycStatus.NEEDS_MORE_INFO) {
            c = existingActive;
        } else {
            throw new IllegalStateException("An active KYC case already exists with status " + existingActive.getStatus());
        }

        c.setDocFrontId(docFrontId);
        c.setDocBackId(docBackId);
        c.setSelfieId(selfieId);
        c.setAddressId(addressId);
        c.setDecisionReason(null);
        c.setReviewerNotes(null);
        c.setReviewedBy(null);
        c.setDecidedAt(null);

        return cases.save(c);
    }

    private static void requireType(Map<UUID, KycUpload> map, UUID id, String expectedType, String fieldName) {
        KycUpload u = map.get(id);
        if (u == null) throw new IllegalArgumentException("Missing upload for " + fieldName);
        if (!expectedType.equalsIgnoreCase(u.getType())) {
            throw new IllegalArgumentException(fieldName + " type mismatch: expected " + expectedType + " but was " + u.getType());
        }
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
    public KycCase decide(String caseId, KycStatus target, String reason, UUID reviewerId) {
        if (!(target == KycStatus.APPROVED || target == KycStatus.REJECTED || target == KycStatus.NEEDS_MORE_INFO)) {
            throw new IllegalArgumentException("Decision must be APPROVED, REJECTED or NEEDS_MORE_INFO");
        }
        KycCase c = cases.findById(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        if (!c.getStatus().canTransitionTo(target)) {
            throw new IllegalArgumentException("Illegal transition from " + c.getStatus() + " to " + target);
        }

        c.setStatus(target);
        c.setDecisionReason(reason);
        c.setReviewerNotes(reason);
        c.setReviewedBy(reviewerId);
        c.setDecidedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return cases.save(c);
    }

    @Override
    @Transactional
    public KycCase markStatus(String caseId, KycStatus status, String note) {
        KycCase c = cases.findById(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (c.getStatus() == status) {
            if (note != null && !note.isBlank()) c.setReviewerNotes(note);
            c.setUpdatedAt(Instant.now());
            return cases.save(c);
        }
        if (!c.getStatus().canTransitionTo(status)) {
            throw new IllegalArgumentException("Illegal transition from " + c.getStatus() + " to " + status);
        }
        c.setStatus(status);
        c.setUpdatedAt(Instant.now());
        if (note != null && !note.isBlank()) {
            c.setReviewerNotes(note);
        }
        return cases.save(c);
    }
}
