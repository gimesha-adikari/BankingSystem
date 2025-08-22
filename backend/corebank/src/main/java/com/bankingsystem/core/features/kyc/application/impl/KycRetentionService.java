package com.bankingsystem.core.features.kyc.application.impl;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.domain.repository.KycCaseRepository;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.modules.common.support.storage.FileStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class KycRetentionService {
    private final KycCaseRepository cases;
    private final FileStorageService files;

    public KycRetentionService(KycCaseRepository cases, FileStorageService files) {
        this.cases = cases; this.files = files;
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void purge() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        List<KycCase> terminal = cases.findAll().stream()
                .filter(c -> (c.getStatus() == KycStatus.APPROVED || c.getStatus() == KycStatus.REJECTED))
                .filter(c -> c.getUpdatedAt().isBefore(cutoff))
                .toList();
        for (var c : terminal) {
            try { files.delete(java.util.UUID.fromString(c.getDocFrontId())); } catch (Exception ignore) {}
            try { files.delete(java.util.UUID.fromString(c.getDocBackId())); } catch (Exception ignore) {}
            try { files.delete(java.util.UUID.fromString(c.getSelfieId())); } catch (Exception ignore) {}
            try { files.delete(java.util.UUID.fromString(c.getAddressId())); } catch (Exception ignore) {}
        }
    }
}