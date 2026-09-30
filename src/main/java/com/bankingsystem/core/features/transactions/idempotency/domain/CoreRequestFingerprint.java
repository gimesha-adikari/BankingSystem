package com.bankingsystem.core.features.transactions.idempotency.domain;

import com.bankingsystem.core.features.ledger.domain.CurrencyCode;
import com.bankingsystem.core.features.ledger.domain.MonetaryAmount;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility for generating canonical SHA-256 semantic fingerprints of financial requests.
 *
 * <p>Canonical representations normalize monetary precision and ensure that extraneous
 * metadata (HTTP headers, timestamps, whitespace) do not alter the financial identity of a request.
 */
public final class CoreRequestFingerprint {

    private CoreRequestFingerprint() {
    }

    /**
     * Computes the semantic fingerprint for a deposit operation:
     * {@code DEPOSIT|<targetAccountId>|<currency>|<normalizedAmount>}
     */
    public static String forDeposit(UUID targetAccountId, CurrencyCode currency, MonetaryAmount amount) {
        Objects.requireNonNull(targetAccountId, "Target account ID must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        String canonical = "DEPOSIT|" + targetAccountId + "|" + currency.getCode() + "|" + amount.getAmount().toPlainString();
        return sha256Hex(canonical);
    }

    /**
     * Computes the semantic fingerprint for a withdrawal operation:
     * {@code WITHDRAWAL|<sourceAccountId>|<currency>|<normalizedAmount>}
     */
    public static String forWithdrawal(UUID sourceAccountId, CurrencyCode currency, MonetaryAmount amount) {
        Objects.requireNonNull(sourceAccountId, "Source account ID must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        String canonical = "WITHDRAWAL|" + sourceAccountId + "|" + currency.getCode() + "|" + amount.getAmount().toPlainString();
        return sha256Hex(canonical);
    }

    /**
     * Computes the semantic fingerprint for a transfer operation:
     * {@code TRANSFER|<sourceAccountId>|<destinationAccountId>|<currency>|<normalizedAmount>}
     *
     * <p>CRITICAL: Source and destination account IDs are intentionally NOT sorted.
     * {@code A -> B} and {@code B -> A} represent distinct financial operations.
     */
    public static String forTransfer(UUID sourceAccountId, UUID destinationAccountId, CurrencyCode currency, MonetaryAmount amount) {
        Objects.requireNonNull(sourceAccountId, "Source account ID must not be null");
        Objects.requireNonNull(destinationAccountId, "Destination account ID must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        String canonical = "TRANSFER|" + sourceAccountId + "|" + destinationAccountId + "|" + currency.getCode() + "|" + amount.getAmount().toPlainString();
        return sha256Hex(canonical);
    }

    /**
     * Computes lowercase 64-character hexadecimal SHA-256 hash of a string.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
