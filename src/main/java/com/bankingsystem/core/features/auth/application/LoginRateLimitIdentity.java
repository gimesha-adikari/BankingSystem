package com.bankingsystem.core.features.auth.application;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record LoginRateLimitIdentity(Type type, String key, UUID accountId) {

    public enum Type {
        ACCOUNT,
        UNKNOWN
    }

    public LoginRateLimitIdentity {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        if (type == Type.ACCOUNT && (accountId == null || !key.equals("account:" + accountId))) {
            throw new IllegalArgumentException("account identity key mismatch");
        }
        if (type == Type.UNKNOWN && (accountId != null || !key.startsWith("unknown:"))) {
            throw new IllegalArgumentException("unknown identity key mismatch");
        }
    }

    public static LoginRateLimitIdentity forAccount(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return new LoginRateLimitIdentity(Type.ACCOUNT, "account:" + accountId, accountId);
    }

    public static LoginRateLimitIdentity forUnknown(String username) {
        String canonicalUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return new LoginRateLimitIdentity(Type.UNKNOWN, "unknown:" + canonicalUsername, null);
    }

    public boolean isAccountIdentity() {
        return type == Type.ACCOUNT;
    }
}
