package com.bankingsystem.core.features.transactions.idempotency.domain;

import com.bankingsystem.core.features.ledger.domain.CurrencyCode;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreRequestFingerprintTest {

    private final UUID accountA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID accountB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void depositFingerprintFormatAndLength() {
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forDeposit(accountA, CurrencyCode.LKR, amount);

        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void semanticMoneyNormalizationProducesIdenticalDepositHash() {
        MonetaryAmount m1 = MonetaryAmount.fromCustomerInput("100", CurrencyCode.LKR);
        MonetaryAmount m2 = MonetaryAmount.fromCustomerInput("100.0", CurrencyCode.LKR);
        MonetaryAmount m3 = MonetaryAmount.fromCustomerInput("100.00", CurrencyCode.LKR);
        MonetaryAmount mDiff = MonetaryAmount.fromCustomerInput("100.01", CurrencyCode.LKR);

        String hash1 = CoreRequestFingerprint.forDeposit(accountA, CurrencyCode.LKR, m1);
        String hash2 = CoreRequestFingerprint.forDeposit(accountA, CurrencyCode.LKR, m2);
        String hash3 = CoreRequestFingerprint.forDeposit(accountA, CurrencyCode.LKR, m3);
        String hashDiff = CoreRequestFingerprint.forDeposit(accountA, CurrencyCode.LKR, mDiff);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isEqualTo(hash3);
        assertThat(hash1).isNotEqualTo(hashDiff);
    }

    @Test
    void fractionalCentsRejectedBeforeFingerprinting() {
        assertThatThrownBy(() -> MonetaryAmount.fromCustomerInput("100.001", CurrencyCode.LKR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdrawalFingerprint() {
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);
        String hash = CoreRequestFingerprint.forWithdrawal(accountA, CurrencyCode.LKR, amount);

        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void transferDirectionHashDiffersBetweenAToBAndBToA() {
        MonetaryAmount amount = MonetaryAmount.fromCustomerInput("50.00", CurrencyCode.LKR);

        String hashAtoB = CoreRequestFingerprint.forTransfer(accountA, accountB, CurrencyCode.LKR, amount);
        String hashBtoA = CoreRequestFingerprint.forTransfer(accountB, accountA, CurrencyCode.LKR, amount);

        assertThat(hashAtoB).matches("^[0-9a-f]{64}$");
        assertThat(hashBtoA).matches("^[0-9a-f]{64}$");
        assertThat(hashAtoB).isNotEqualTo(hashBtoA);
    }
}
