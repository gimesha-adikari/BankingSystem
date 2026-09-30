package com.bankingsystem.core.features.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonetaryAmountTest {

    private final CurrencyCode lkr = CurrencyCode.LKR;

    @Test
    void customerInputParsesAndNormalizesToScaleFour() {
        MonetaryAmount m1 = MonetaryAmount.fromCustomerInput("10", lkr);
        assertThat(m1.getAmount()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(m1.getAmount().scale()).isEqualTo(4);

        MonetaryAmount m2 = MonetaryAmount.fromCustomerInput("10.5", lkr);
        assertThat(m2.getAmount()).isEqualByComparingTo(new BigDecimal("10.5000"));
        assertThat(m2.getAmount().scale()).isEqualTo(4);

        MonetaryAmount m3 = MonetaryAmount.fromCustomerInput("10.50", lkr);
        assertThat(m3.getAmount()).isEqualByComparingTo(new BigDecimal("10.5000"));
        assertThat(m3.getAmount().scale()).isEqualTo(4);
    }

    @Test
    void customerInputRejectsMoreThanTwoDecimalsWithoutRounding() {
        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("10.999", lkr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 2 fractional digits");

        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("12.3456", lkr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 2 fractional digits");
    }

    @Test
    void customerInputRejectsZeroNegativeAndNonDecimalStrings() {
        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("0", lkr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly positive");

        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("-10.50", lkr))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("1e2", lkr))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("$10.00", lkr))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("", lkr))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput(null, lkr))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ledgerAmountPreservesExactScaleFourValue() {
        BigDecimal val = new BigDecimal("12.3456");
        MonetaryAmount ledgerAmount = MonetaryAmount.fromLedger(val, lkr);

        assertThat(ledgerAmount.getAmount()).isEqualByComparingTo(val);
        assertThat(ledgerAmount.getAmount().scale()).isEqualTo(4);
        assertThat(ledgerAmount.toString()).isEqualTo("12.3456 LKR");
    }

    @Test
    void ledgerAmountRejectsMoreThanScaleFour() {
        BigDecimal val = new BigDecimal("12.34567");
        assertThatThrownBy(() -> MonetaryAmount.fromLedger(val, lkr))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 4 fractional digits");
    }

    @Test
    void arithmeticAndCurrencyEnforcement() {
        MonetaryAmount a = MonetaryAmount.fromCustomerInput("50.00", lkr);
        MonetaryAmount b = MonetaryAmount.fromCustomerInput("25.50", lkr);

        MonetaryAmount sum = a.add(b);
        assertThat(sum.getAmount()).isEqualByComparingTo(new BigDecimal("75.5000"));

        MonetaryAmount diff = a.subtract(b);
        assertThat(diff.getAmount()).isEqualByComparingTo(new BigDecimal("24.5000"));

        MonetaryAmount usd = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.USD);
        assertThatThrownBy(() -> a.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");

        assertThatThrownBy(() -> a.subtract(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }
}
