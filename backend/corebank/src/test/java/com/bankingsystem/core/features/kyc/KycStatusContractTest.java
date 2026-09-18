package com.bankingsystem.core.features.kyc;

import com.bankingsystem.core.modules.common.enums.KycStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KycStatusContractTest {

    @Test
    void pendingCasesCanEnterAutomationOrHumanReview() {
        assertThat(KycStatus.PENDING.canTransitionTo(KycStatus.AUTO_REVIEW)).isTrue();
        assertThat(KycStatus.PENDING.canTransitionTo(KycStatus.UNDER_REVIEW)).isTrue();
        assertThat(KycStatus.PENDING.canTransitionTo(KycStatus.APPROVED)).isFalse();
    }

    @Test
    void terminalCasesCannotBeReopened() {
        assertThat(KycStatus.APPROVED.canTransitionTo(KycStatus.PENDING)).isFalse();
        assertThat(KycStatus.REJECTED.canTransitionTo(KycStatus.UNDER_REVIEW)).isFalse();
    }
}
