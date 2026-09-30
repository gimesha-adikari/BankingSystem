package com.bankingsystem.core.features.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyCodeTest {

    @Test
    void validCurrencyCodesAreAccepted() {
        CurrencyCode lkr = CurrencyCode.of("LKR");
        assertThat(lkr.getCode()).isEqualTo("LKR");
        assertThat(lkr.toString()).isEqualTo("LKR");
        assertThat(lkr).isEqualTo(CurrencyCode.LKR);

        CurrencyCode usd = CurrencyCode.of("USD");
        assertThat(usd.getCode()).isEqualTo("USD");

        CurrencyCode eur = CurrencyCode.of("EUR");
        assertThat(eur.getCode()).isEqualTo("EUR");
    }

    @Test
    void lowercaseAndMixedCaseAreRejected() {
        assertThatThrownBy(() -> CurrencyCode.of("lkr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3 uppercase ASCII letters");

        assertThatThrownBy(() -> CurrencyCode.of("Lkr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3 uppercase ASCII letters");

        assertThatThrownBy(() -> CurrencyCode.of("usd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3 uppercase ASCII letters");
    }

    @Test
    void malformedLengthAndInvalidCharactersAreRejected() {
        assertThatThrownBy(() -> CurrencyCode.of("LK"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CurrencyCode.of("LKRR"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CurrencyCode.of("123"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CurrencyCode.of("LK$"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CurrencyCode.of("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CurrencyCode.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void nonIsoCurrencyIsRejected() {
        assertThatThrownBy(() -> CurrencyCode.of("ZZZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a recognized ISO-4217 currency");
    }

    @Test
    void equalityAndComparison() {
        CurrencyCode a = CurrencyCode.of("LKR");
        CurrencyCode b = CurrencyCode.LKR;
        CurrencyCode c = CurrencyCode.USD;

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
        assertThat(a.compareTo(c)).isLessThan(0);
    }
}
