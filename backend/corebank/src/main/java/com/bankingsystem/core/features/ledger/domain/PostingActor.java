package com.bankingsystem.core.features.ledger.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Type-safe polymorphic actor representation guaranteeing valid USER or SYSTEM actor configurations.
 */
public sealed interface PostingActor extends Serializable permits PostingActor.UserActor, PostingActor.SystemActor {

    LedgerActorType getActorType();

    default UUID getUserId() {
        return null;
    }

    default String getSystemActorId() {
        return null;
    }

    static PostingActor user(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null for UserActor");
        }
        return new UserActor(userId);
    }

    static PostingActor system(String systemActorId) {
        if (systemActorId == null || systemActorId.isBlank()) {
            throw new IllegalArgumentException("System actor ID cannot be null or blank for SystemActor");
        }
        if (systemActorId.length() > 50) {
            throw new IllegalArgumentException("System actor ID cannot exceed 50 characters: " + systemActorId);
        }
        return new SystemActor(systemActorId.trim());
    }

    final class UserActor implements PostingActor {
        private static final long serialVersionUID = 1L;
        private final UUID userId;

        public UserActor(UUID userId) {
            this.userId = Objects.requireNonNull(userId, "userId");
        }

        @Override
        public LedgerActorType getActorType() {
            return LedgerActorType.USER;
        }

        @Override
        public UUID getUserId() {
            return userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserActor userActor = (UserActor) o;
            return Objects.equals(userId, userActor.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }

        @Override
        public String toString() {
            return "UserActor[" + userId + "]";
        }
    }

    final class SystemActor implements PostingActor {
        private static final long serialVersionUID = 1L;
        private final String systemActorId;

        public SystemActor(String systemActorId) {
            this.systemActorId = Objects.requireNonNull(systemActorId, "systemActorId");
        }

        @Override
        public LedgerActorType getActorType() {
            return LedgerActorType.SYSTEM;
        }

        @Override
        public String getSystemActorId() {
            return systemActorId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SystemActor that = (SystemActor) o;
            return Objects.equals(systemActorId, that.systemActorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(systemActorId);
        }

        @Override
        public String toString() {
            return "SystemActor[" + systemActorId + "]";
        }
    }
}
