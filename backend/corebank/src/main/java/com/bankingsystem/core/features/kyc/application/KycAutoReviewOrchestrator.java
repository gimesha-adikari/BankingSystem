package com.bankingsystem.core.features.kyc.application;

import com.bankingsystem.core.features.kyc.domain.KycCase;

public interface KycAutoReviewOrchestrator {

    /**
     * Run ML auto-review for a single case and return the updated case.
     */
    KycCase run(String caseId);

    /**
     * Periodic batch processor (scheduled in the impl).
     */
    void runBatch();
}
