package com.bankingsystem.core.features.kyc.application.impl;

import com.bankingsystem.core.features.kyc.application.KycAutoReviewOrchestrator;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.domain.KycCheck;
import com.bankingsystem.core.features.kyc.domain.repository.KycCaseRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycCheckRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import com.bankingsystem.core.features.kyc.integration.MlKycClient;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.modules.common.support.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KycAutoReviewOrchestratorImpl implements KycAutoReviewOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(KycAutoReviewOrchestratorImpl.class);
    private static final UUID SYSTEM_REVIEWER = new UUID(0L, 0L);

    private final KycCaseRepository cases;
    private final KycCheckRepository checks;
    private final KycUploadRepository uploads;
    private final FileStorageService files;
    private final KycCaseService caseService;
    private final MlKycClient ml;
    private final ObjectMapper mapper;

    public KycAutoReviewOrchestratorImpl(
            KycCaseRepository cases,
            KycCheckRepository checks,
            KycUploadRepository uploads,
            FileStorageService files,
            KycCaseService caseService,
            MlKycClient ml,
            ObjectMapper mapper
    ) {
        this.cases = cases;
        this.checks = checks;
        this.uploads = uploads;
        this.files = files;
        this.caseService = caseService;
        this.ml = ml;
        this.mapper = mapper;
    }

    @Override
    @Scheduled(fixedDelayString = "${kyc.auto-review.fixed-delay-ms:10000}")
    @Transactional
    public void runBatch() {
        var candidates = cases.findCandidatesForAutoReview(
                List.of(KycStatus.PENDING),
                PageRequest.of(0, 50)
        );

        for (KycCase c : candidates) {
            if (cases.tryMarkProcessing(c.getId()) == 1) {
                try {
                    run(c.getId());
                } catch (Exception ex) {
                    log.error("Failed to process KYC case {}: {}", c.getId(), ex.getMessage(), ex);
                    try {
                        caseService.markStatus(c.getId(), KycStatus.UNDER_REVIEW, "Auto-review failed: " + ex.getMessage());
                    } catch (Exception e) {
                        log.error("Failed to mark case {} as failed: {}", c.getId(), e.getMessage(), e);
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public KycCase run(String caseId) {
        KycCase c = cases.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        if (!c.isProcessing()) {
            if (cases.tryMarkProcessing(c.getId()) != 1) {
                return c;
            }
            c.setProcessing(true);
        }

        switch (c.getStatus()) {
            case PENDING -> caseService.markStatus(c.getId(), KycStatus.AUTO_REVIEW, "Automation started");
            case AUTO_REVIEW -> { /* continue */ }
            default -> {
                c.setProcessing(false);
                cases.save(c);
                return c;
            }
        }

        String selfieB64 = readB64(c.getSelfieId());
        String frontB64  = readB64(c.getDocFrontId());
        String backB64   = readB64(c.getDocBackId());
        String billB64   = readB64(c.getAddressId());

        MlKycClient.KycAggregateRequest req = new MlKycClient.KycAggregateRequest(
                selfieB64, null, frontB64, backB64, billB64,
                Map.of("caseId", c.getId())
        );

        try {
            var res = ml.aggregate(req);

            if (res.body != null && res.body.checks != null) {
                for (var ch : res.body.checks) {
                    KycCheck k = new KycCheck();
                    k.setCaseId(c.getId());
                    k.setType(ch.type);
                    k.setScore(ch.score);
                    k.setPassed(ch.passed);
                    try {
                        k.setDetailsJson(mapper.writeValueAsString(ch.details == null ? Map.of() : ch.details));
                    } catch (Exception ex) {
                        k.setDetailsJson("{}");
                    }
                    k.setCreatedAt(Instant.now());
                    checks.save(k);
                }
            }

            if (res.body != null && res.body.decision != null) {
                switch (res.body.decision) {
                    case "APPROVE" -> caseService.decide(c.getId(), KycStatus.APPROVED, "auto_approved", SYSTEM_REVIEWER);
                    case "REJECT" -> caseService.decide(c.getId(), KycStatus.REJECTED,
                            String.join(";", res.body.reasons == null ? List.of() : res.body.reasons), SYSTEM_REVIEWER);
                    default -> caseService.markStatus(c.getId(), KycStatus.UNDER_REVIEW,
                            String.join(";", res.body.reasons == null ? List.of() : res.body.reasons));
                }
            } else {
                caseService.markStatus(c.getId(), KycStatus.UNDER_REVIEW, "ml_response_empty");
            }

        } catch (Exception ex) {
            KycCheck err = new KycCheck();
            err.setCaseId(c.getId());
            err.setType("ERROR");
            err.setScore(0.0);
            err.setPassed(false);
            err.setDetailsJson("{\"error\":\"" + safe(ex.getMessage()) + "\"}");
            err.setCreatedAt(Instant.now());
            checks.save(err);

            caseService.markStatus(c.getId(), KycStatus.UNDER_REVIEW, "ml_unavailable");
        } finally {
            cases.findById(caseId).ifPresent(k -> {
                k.setProcessing(false);
                cases.save(k);
            });
        }

        return cases.findById(caseId).orElse(c);
    }

    private String readB64(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return null;
        try {
            byte[] data = files.read(UUID.fromString(uuidStr));
            if (data == null || data.length == 0) return null;
            if (data.length > 6 * 1024 * 1024) return null;
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) { return s == null ? "" : s.replace("\"", "'"); }
}
