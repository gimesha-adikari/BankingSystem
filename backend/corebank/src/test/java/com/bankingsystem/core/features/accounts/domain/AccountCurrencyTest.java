package com.bankingsystem.core.features.accounts.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountCurrencyTest {

    @Test
    void newlyConstructedAccountDefaultsToLkr() {
        Account account = new Account();
        assertThat(account.getCurrency()).isEqualTo("LKR");
    }

    @Test
    void currencyFieldCanBeExplicitlySet() {
        Account account = new Account();
        account.setCurrency("USD");
        assertThat(account.getCurrency()).isEqualTo("USD");
    }
}
