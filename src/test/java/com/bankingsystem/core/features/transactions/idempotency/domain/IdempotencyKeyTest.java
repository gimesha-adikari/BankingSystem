package com.bankingsystem.core.features.transactions.idempotency.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTest {

    @Test
    void validKeyConstructsSuccessfully() {
        IdempotencyKey key = IdempotencyKey.of("abc-123_XYZ");
        assertThat(key.getValue()).isEqualTo("abc-123_XYZ");
        assertThat(key.toString()).isEqualTo("abc-123_XYZ");
    }

    @Test
    void nullKeyThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> IdempotencyKey.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or empty");
    }

    @Test
    void emptyKeyThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> IdempotencyKey.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or empty");
    }

    @Test
    void leadingOrTrailingWhitespaceThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> IdempotencyKey.of("  abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leading or trailing whitespace");

        assertThatThrownBy(() -> IdempotencyKey.of("abc  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leading or trailing whitespace");
    }

    @Test
    void keyExceeding128CharactersThrowsIllegalArgumentException() {
        String longKey = "a".repeat(129);
        assertThatThrownBy(() -> IdempotencyKey.of(longKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 128 characters");

        // 128 chars is allowed
        IdempotencyKey maxKey = IdempotencyKey.of("a".repeat(128));
        assertThat(maxKey.getValue()).hasSize(128);
    }

    @Test
    void keyWithControlCharactersThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> IdempotencyKey.of("abc\n123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control characters");

        assertThatThrownBy(() -> IdempotencyKey.of("abc\u0000def"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control characters");

        assertThatThrownBy(() -> IdempotencyKey.of("abc\u007Fdef"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("control characters");
    }

    @Test
    void caseInsensitiveEqualityAndHashCodeMatchesDatabaseCollation() {
        IdempotencyKey keyUpper = IdempotencyKey.of("KEY-ABC-123");
        IdempotencyKey keyLower = IdempotencyKey.of("key-abc-123");
        IdempotencyKey keyMixed = IdempotencyKey.of("Key-Abc-123");

        assertThat(keyUpper).isEqualTo(keyLower);
        assertThat(keyUpper).isEqualTo(keyMixed);
        assertThat(keyUpper.hashCode()).isEqualTo(keyLower.hashCode());
        assertThat(keyUpper.hashCode()).isEqualTo(keyMixed.hashCode());

        IdempotencyKey differentKey = IdempotencyKey.of("key-xyz-789");
        assertThat(keyUpper).isNotEqualTo(differentKey);
    }

    @Test
    void nonAsciiCharactersAreRejectedToAvoidAccentCollationMismatch() {
        assertThatThrownBy(() -> IdempotencyKey.of("café"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ASCII letters");

        assertThatThrownBy(() -> IdempotencyKey.of("ключ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ASCII letters");
    }

    @Test
    void supportedPunctuationIsAccepted() {
        IdempotencyKey key = IdempotencyKey.of("dep-001_A.B:C");
        assertThat(key.getValue()).isEqualTo("dep-001_A.B:C");
    }
}
