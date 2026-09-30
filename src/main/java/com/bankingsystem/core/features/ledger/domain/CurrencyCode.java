package com.bankingsystem.core.features.ledger.domain;

import java.io.Serializable;
import java.util.Currency;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable ISO-4217 currency code value object.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Exactly 3 uppercase ASCII letters matching {@code ^[A-Z]{3}$}.</li>
 *   <li>Rejects lowercase, mixed-case, numbers, whitespace, and symbols.</li>
 *   <li>Validates against Java's ISO-4217 registry ({@link Currency#getInstance(String)}).</li>
 * </ul>
 */
public final class CurrencyCode implements Serializable, Comparable<CurrencyCode> {

    private static final long serialVersionUID = 1L;

    private static final Pattern FORMAT_PATTERN = Pattern.compile("^[A-Z]{3}$");

    public static final CurrencyCode LKR = new CurrencyCode("LKR");
    public static final CurrencyCode USD = new CurrencyCode("USD");
    public static final CurrencyCode EUR = new CurrencyCode("EUR");
    public static final CurrencyCode GBP = new CurrencyCode("GBP");

    private final String code;

    private CurrencyCode(String code) {
        this.code = code;
    }

    /**
     * Parse and validate a raw currency string.
     *
     * @param raw the currency string, must be exactly 3 uppercase ASCII letters in ISO-4217
     * @return the immutable CurrencyCode instance
     * @throws IllegalArgumentException if null, malformed, or not in ISO-4217 registry
     */
    public static CurrencyCode of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Currency code cannot be null");
        }
        if (!FORMAT_PATTERN.matcher(raw).matches()) {
            throw new IllegalArgumentException(
                    "Currency code must consist of exactly 3 uppercase ASCII letters, got: '" + raw + "'");
        }
        try {
            Currency.getInstance(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Currency code is not a recognized ISO-4217 currency: '" + raw + "'", e);
        }
        if ("LKR".equals(raw)) return LKR;
        if ("USD".equals(raw)) return USD;
        if ("EUR".equals(raw)) return EUR;
        if ("GBP".equals(raw)) return GBP;
        return new CurrencyCode(raw);
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyCode that = (CurrencyCode) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public int compareTo(CurrencyCode o) {
        return this.code.compareTo(o.code);
    }
}
