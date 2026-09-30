package com.bankingsystem.core.modules.common.enums;

public enum KycStatus {
    PENDING,         // user submitted, queued for automation
    AUTO_REVIEW,     // ML checks running / pending
    UNDER_REVIEW,    // human reviewer queue
    NEEDS_MORE_INFO, // RFI loop with customer
    APPROVED,
    REJECTED,
    SUPERSEDED;      // replaced by a newer case

    public boolean canTransitionTo(KycStatus target) {
        switch (this) {
            case PENDING:
                return target == AUTO_REVIEW || target == UNDER_REVIEW || target == NEEDS_MORE_INFO;
            case AUTO_REVIEW:
                return target == UNDER_REVIEW || target == NEEDS_MORE_INFO || target == APPROVED || target == REJECTED;
            case UNDER_REVIEW:
                return target == APPROVED || target == REJECTED || target == NEEDS_MORE_INFO;
            case NEEDS_MORE_INFO:
                return target == UNDER_REVIEW || target == REJECTED;
            default: // APPROVED, REJECTED, SUPERSEDED
                return false;
        }
    }
}
