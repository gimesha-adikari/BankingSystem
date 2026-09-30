package com.bankingsystem.core.features.transactions.idempotency.domain;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated value object representing a client idempotency key.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Non-null, non-blank</li>
 *   <li>Maximum 128 characters</li>
 *   <li>No control characters (ASCII &lt; 32 or 127)</li>
 *   <li>Explicit whitespace policy: leading/trailing whitespace is NOT silently trimmed; it is rejected</li>
 *   <li>ASCII-safe characters only, so Java equality and MySQL {@code utf8mb4_0900_ai_ci} comparison cannot diverge on accent folding</li>
 *   <li>Case-insensitive equality and hash code to match the database's case-insensitive comparison</li>
 * </ul>
 */
public final class IdempotencyKey implements Serializable {

    private static final int MAX_LENGTH = 128;
    private static final Pattern SUPPORTED_CHARACTERS = Pattern.compile("^[A-Za-z0-9._:-]+$");
    private final String value;

    private IdempotencyKey(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Idempotency key must not be null or empty");
        }
        if (value.startsWith(" ") || value.endsWith(" ") || !value.equals(value.trim())) {
            throw new IllegalArgumentException("Idempotency key must not contain leading or trailing whitespace");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Idempotency key must not exceed " + MAX_LENGTH + " characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 32 || c == 127) {
                throw new IllegalArgumentException("Idempotency key must not contain control characters");
            }
        }
        if (!SUPPORTED_CHARACTERS.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency key may contain only ASCII letters, digits, '.', '_', ':', or '-'");
        }
        this.value = value;
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKey that = (IdempotencyKey) o;
        return this.value.equalsIgnoreCase(that.value);
    }

    @Override
    public int hashCode() {
        return value.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
