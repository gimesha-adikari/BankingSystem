package com.bankingsystem.core.features.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized domain authority for debit-credit sign semantics across account classes.
 *
 * <p>Semantic Matrix:
 * <table border="1">
 *   <tr><th>Account Class</th><th>Direction</th><th>Balance Effect</th></tr>
 *   <tr><td>ASSET</td><td>DEBIT</td><td>+ Amount</td></tr>
 *   <tr><td>ASSET</td><td>CREDIT</td><td>- Amount</td></tr>
 *   <tr><td>LIABILITY</td><td>DEBIT</td><td>- Amount</td></tr>
 *   <tr><td>LIABILITY</td><td>CREDIT</td><td>+ Amount</td></tr>
 *   <tr><td>EQUITY</td><td>DEBIT</td><td>- Amount</td></tr>
 *   <tr><td>EQUITY</td><td>CREDIT</td><td>+ Amount</td></tr>
 * </table>
 *
 * <p>Customer deposit accounts are modeled as {@link LedgerAccountClass#LIABILITY}.
 * Therefore, a CREDIT posting increases the customer's balance, and a DEBIT posting decreases it.
 */
public final class LedgerMath {

    private LedgerMath() {}

    /**
     * Calculate the signed balance delta for a given account class, posting direction, and amount.
     *
     * @param accountClass the ledger account class (ASSET, LIABILITY, EQUITY)
     * @param direction the posting direction (DEBIT, CREDIT)
     * @param amount the positive leg magnitude (scale 4)
     * @return the signed balance delta to be added to the account's existing balance
     */
    public static BigDecimal balanceDelta(LedgerAccountClass accountClass, PostingDirection direction, BigDecimal amount) {
        if (accountClass == null) {
            throw new IllegalArgumentException("Account class cannot be null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Posting direction cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Posting amount must be non-negative, got: " + amount);
        }

        BigDecimal scaled = amount.setScale(MonetaryAmount.STORAGE_SCALE, RoundingMode.UNNECESSARY);

        switch (accountClass) {
            case ASSET:
                return (direction == PostingDirection.DEBIT) ? scaled : scaled.negate();
            case LIABILITY:
            case EQUITY:
                return (direction == PostingDirection.CREDIT) ? scaled : scaled.negate();
            default:
                throw new IllegalArgumentException("Unsupported account class: " + accountClass);
        }
    }

    /**
     * Convenience method calculating balance delta from a MonetaryAmount.
     */
    public static BigDecimal balanceDelta(LedgerAccountClass accountClass, PostingDirection direction, MonetaryAmount monetaryAmount) {
        if (monetaryAmount == null) {
            throw new IllegalArgumentException("MonetaryAmount cannot be null");
        }
        return balanceDelta(accountClass, direction, monetaryAmount.getAmount());
    }
}
