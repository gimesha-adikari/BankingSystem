package com.bankingsystem.core.features.ledger.application;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Structured report produced by {@link LedgerReconciliationService}.
 */
public final class LedgerReconciliationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public record AccountDiscrepancy(
            UUID accountId,
            String accountNumber,
            BigDecimal materializedBalance,
            BigDecimal derivedBalance,
            BigDecimal difference
    ) implements Serializable {}

    private final int totalAccountsChecked;
    private final int reconciledAccountsCount;
    private final List<AccountDiscrepancy> discrepancies;

    public LedgerReconciliationResult(int totalAccountsChecked, int reconciledAccountsCount,
                                      List<AccountDiscrepancy> discrepancies) {
        this.totalAccountsChecked = totalAccountsChecked;
        this.reconciledAccountsCount = reconciledAccountsCount;
        this.discrepancies = discrepancies != null ? Collections.unmodifiableList(discrepancies) : Collections.emptyList();
    }

    public int getTotalAccountsChecked() {
        return totalAccountsChecked;
    }

    public int getReconciledAccountsCount() {
        return reconciledAccountsCount;
    }

    public int getDiscrepancyCount() {
        return discrepancies.size();
    }

    public List<AccountDiscrepancy> getDiscrepancies() {
        return discrepancies;
    }

    public boolean isClean() {
        return discrepancies.isEmpty();
    }

    @Override
    public String toString() {
        return "LedgerReconciliationResult[total=" + totalAccountsChecked +
                ", reconciled=" + reconciledAccountsCount +
                ", discrepancies=" + discrepancies.size() +
                ", clean=" + isClean() + "]";
    }
}
