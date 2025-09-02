package com.bankingsystem.core.features.wallet.application;

import com.bankingsystem.core.features.wallet.domain.entity.IdempotencyKey;
import com.bankingsystem.core.features.wallet.domain.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repo;
    private final ObjectMapper mapper;

    @Transactional
    public <T> T withIdempotency(String key, Object request, Class<T> type, Supplier<T> supplier) {
        if (key == null || key.isBlank()) return supplier.get();
        String reqJson = toJson(request);
        String hash = sha256(reqJson);
        Optional<IdempotencyKey> existing = repo.findById(key);
        if (existing.isPresent()) {
            IdempotencyKey i = existing.get();
            if (hash.equals(i.getRequestHash()) && i.getResponseJson() != null) {
                return fromJson(i.getResponseJson(), type);
            }
        }
        T result = supplier.get();
        String resJson = toJson(result);
        IdempotencyKey rec = existing.orElseGet(() -> IdempotencyKey.builder().idemKey(key).build());
        rec.setRequestHash(hash);
        rec.setResponseJson(resJson);
        if (rec.getCreatedAt() == null) rec.setCreatedAt(Instant.now());
        repo.save(rec);
        return result;
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
