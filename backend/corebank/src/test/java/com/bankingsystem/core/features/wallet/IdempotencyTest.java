package com.bankingsystem.core.features.wallet;

import com.bankingsystem.core.features.wallet.application.IdempotencyService;
import com.bankingsystem.core.features.wallet.domain.entity.IdempotencyKey;
import com.bankingsystem.core.features.wallet.domain.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class IdempotencyTest {

    IdempotencyKeyRepository repo = mock(IdempotencyKeyRepository.class);
    ObjectMapper mapper = new ObjectMapper();
    IdempotencyService service;

    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();
    String key = "test-key-1";
    String operation = "QR_PAYMENT";

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repo, mapper);
    }

    @Test
    void sameUserSameKeyReturnsCachedResponse() {
        String storageKey = userA + ":" + operation + ":" + key;
        String requestJson = "\"test-request\"";
        IdempotencyKey cached = new IdempotencyKey();
        cached.setIdemKey(storageKey);
        cached.setRequestHash(sha256(requestJson));
        cached.setResponseJson("\"original-result\"");
        when(repo.findById(storageKey)).thenReturn(Optional.of(cached));

        String result = service.withIdempotency(userA, key, operation, "test-request", String.class, () -> "new-result");

        assertThat(result).isEqualTo("original-result");
        verify(repo, never()).save(any());
    }

    @Test
    void differentUserSameKeyExecutesIndependently() {
        String storageKeyB = userB + ":" + operation + ":" + key;
        when(repo.findById(storageKeyB)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = service.withIdempotency(userB, key, operation, "test-request", String.class, () -> "user-b-result");

        assertThat(result).isEqualTo("user-b-result");
        verify(repo).save(argThat(k -> k.getIdemKey().equals(storageKeyB)));
    }

    @Test
    void nullKeyBypassesIdempotency() {
        String result = service.withIdempotency(userA, null, operation, "test-request", String.class, () -> "bypassed");
        assertThat(result).isEqualTo("bypassed");
        verifyNoInteractions(repo);
    }

    @Test
    void blankKeyBypassesIdempotency() {
        String result = service.withIdempotency(userA, "   ", operation, "test-request", String.class, () -> "bypassed");
        assertThat(result).isEqualTo("bypassed");
        verifyNoInteractions(repo);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
