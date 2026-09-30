package com.bankingsystem.core.features.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerMathTest {

    private final BigDecimal amount = new BigDecimal("100.0000");

    @Test
    void assetDebitIncreasesAndCreditDecreases() {
        BigDecimal debitDelta = LedgerMath.balanceDelta(LedgerAccountClass.ASSET, PostingDirection.DEBIT, amount);
        assertThat(debitDelta).isEqualByComparingTo(new BigDecimal("100.0000"));

        BigDecimal creditDelta = LedgerMath.balanceDelta(LedgerAccountClass.ASSET, PostingDirection.CREDIT, amount);
        assertThat(creditDelta).isEqualByComparingTo(new BigDecimal("-100.0000"));
    }

    @Test
    void liabilityDebitDecreasesAndCreditIncreases() {
        BigDecimal debitDelta = LedgerMath.balanceDelta(LedgerAccountClass.LIABILITY, PostingDirection.DEBIT, amount);
        assertThat(debitDelta).isEqualByComparingTo(new BigDecimal("-100.0000"));

        BigDecimal creditDelta = LedgerMath.balanceDelta(LedgerAccountClass.LIABILITY, PostingDirection.CREDIT, amount);
        assertThat(creditDelta).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void equityDebitDecreasesAndCreditIncreases() {
        BigDecimal debitDelta = LedgerMath.balanceDelta(LedgerAccountClass.EQUITY, PostingDirection.DEBIT, amount);
        assertThat(debitDelta).isEqualByComparingTo(new BigDecimal("-100.0000"));

        BigDecimal creditDelta = LedgerMath.balanceDelta(LedgerAccountClass.EQUITY, PostingDirection.CREDIT, amount);
        assertThat(creditDelta).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void nullAndNegativeArgumentsAreRejected() {
        assertThatThrownBy(() -> LedgerMath.balanceDelta(null, PostingDirection.DEBIT, amount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerMath.balanceDelta(LedgerAccountClass.LIABILITY, null, amount))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerMath.balanceDelta(LedgerAccountClass.LIABILITY, PostingDirection.DEBIT, (BigDecimal) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> LedgerMath.balanceDelta(LedgerAccountClass.LIABILITY, PostingDirection.DEBIT, new BigDecimal("-10.0000")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
