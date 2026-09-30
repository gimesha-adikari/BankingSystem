package com.bankingsystem.core.features.ledger.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable monetary amount value object representing financial value at scale 4.
 *
 * <p>Scale Policy:
 * <ul>
 *   <li>Posting & Ledger Storage Scale: strictly {@code 4} (e.g. {@code 10.0000}, {@code 12.3456}).</li>
 *   <li>Customer / External Input Scale: at most {@code 2} fractional decimal places (e.g. {@code 10.50}).</li>
 *   <li>Calculation Scale (future calculated products): 8 decimal places with HALF_EVEN rounding.</li>
 * </ul>
 *
 * <p>Never uses binary floating point ({@code double} / {@code float}) types.
 */
public final class MonetaryAmount implements Serializable, Comparable<MonetaryAmount> {

    private static final long serialVersionUID = 1L;

    public static final int STORAGE_SCALE = 4;
    public static final int CUSTOMER_INPUT_MAX_SCALE = 2;

    private static final Pattern STRICT_DECIMAL_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");

    private final BigDecimal amount;
    private final CurrencyCode currency;

    private MonetaryAmount(BigDecimal amount, CurrencyCode currency) {
        this.amount = amount;
        this.currency = currency;
    }

    /**
     * Create MonetaryAmount from customer or API input.
     *
     * <p>Rules:
     * <ul>
     *   <li>Must be decimal text only (no exponents/scientific notation, no currency symbols).</li>
     *   <li>Must be strictly positive (amount > 0).</li>
     *   <li>Must have at most 2 fractional decimal places.</li>
     *   <li>Never silently rounds; normalizes to storage scale 4 (e.g. "10" -> 10.0000, "10.5" -> 10.5000).</li>
     * </ul>
     *
     * @param decimalText the raw decimal text
     * @param currency the currency code
     * @return normalized MonetaryAmount at scale 4
     */
    public static MonetaryAmount fromCustomerInput(String decimalText, CurrencyCode currency) {
        if (decimalText == null || decimalText.isBlank()) {
            throw new IllegalArgumentException("Customer amount input cannot be null or blank");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        String trimmed = decimalText.trim();
        if (!STRICT_DECIMAL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Customer amount must be a plain positive decimal without scientific notation or symbols, got: '" + decimalText + "'");
        }
        BigDecimal parsed = new BigDecimal(trimmed);
        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Customer amount must be strictly positive (> 0), got: " + parsed);
        }
        if (parsed.scale() > CUSTOMER_INPUT_MAX_SCALE) {
            throw new IllegalArgumentException(
                    "Customer amount cannot exceed " + CUSTOMER_INPUT_MAX_SCALE + " fractional digits, got: " + parsed.scale() + " for '" + decimalText + "'");
        }
        BigDecimal scaled = parsed.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY);
        return new MonetaryAmount(scaled, currency);
    }

    /**
     * Create MonetaryAmount from ledger or internal stored BigDecimal.
     *
     * <p>Rules:
     * <ul>
     *   <li>Must be strictly positive (amount > 0).</li>
     *   <li>Must have at most 4 fractional decimal places.</li>
     *   <li>Normalizes to scale 4 using {@link RoundingMode#UNNECESSARY} to preserve exact precision (e.g. 12.3456).</li>
     * </ul>
     *
     * @param amount the exact BigDecimal value
     * @param currency the currency code
     * @return normalized MonetaryAmount at scale 4
     */
    public static MonetaryAmount fromLedger(BigDecimal amount, CurrencyCode currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Ledger amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger amount must be strictly positive (> 0), got: " + amount);
        }
        if (amount.scale() > STORAGE_SCALE) {
            throw new IllegalArgumentException(
                    "Ledger amount cannot exceed " + STORAGE_SCALE + " fractional digits, got: " + amount.scale() + " for " + amount);
        }
        BigDecimal scaled = amount.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY);
        return new MonetaryAmount(scaled, currency);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public MonetaryAmount add(MonetaryAmount other) {
        requireSameCurrency(other);
        return new MonetaryAmount(this.amount.add(other.amount).setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY), this.currency);
    }

    public MonetaryAmount subtract(MonetaryAmount other) {
        requireSameCurrency(other);
        BigDecimal res = this.amount.subtract(other.amount).setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY);
        if (res.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Resulting amount must be strictly positive (> 0), got: " + res);
        }
        return new MonetaryAmount(res, this.currency);
    }

    public void requireSameCurrency(MonetaryAmount other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare or operate with null MonetaryAmount");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(MonetaryAmount o) {
        requireSameCurrency(o);
        return this.amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonetaryAmount that = (MonetaryAmount) o;
        return Objects.equals(currency, that.currency) && this.amount.compareTo(that.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCode();
    }
}
