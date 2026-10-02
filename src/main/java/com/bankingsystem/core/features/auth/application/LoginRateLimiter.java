package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.modules.common.config.LoginRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LoginRateLimiter {

    private static final String UNKNOWN_ADDRESS = "unknown";

    private final LoginRateLimitProperties properties;
    private final Ticker ticker;
    private final Cache<String, BucketState> ipBuckets;
    private final Cache<String, BucketState> usernameBuckets;
    private final Cache<String, BucketState> pairBuckets;

    @Autowired
    public LoginRateLimiter(LoginRateLimitProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    public LoginRateLimiter(LoginRateLimitProperties properties, Ticker ticker) {
        properties.validate();
        this.properties = properties;
        this.ticker = ticker;
        this.ipBuckets = createCache(properties, ticker);
        this.usernameBuckets = createCache(properties, ticker);
        this.pairBuckets = createCache(properties, ticker);
    }

    public Admission tryAdmit(String remoteAddress, String username) {
        if (!properties.isEnabled()) {
            return Admission.permitted();
        }
        try {
            String addressKey = canonicalAddress(remoteAddress);
            String usernameKey = canonicalUsername(username);
            String pairKey = addressKey + "\u0000" + usernameKey;

            Decision ipDecision = consume(ipBuckets, addressKey, properties.getIpCapacity(), properties.getIpRefillPeriod());
            if (ipDecision.retryAfterNanos() > 0) {
                return new Admission(false, toRetryAfterSeconds(ipDecision.retryAfterNanos()));
            }
            Decision usernameDecision = consume(usernameBuckets, usernameKey, properties.getUsernameCapacity(), properties.getUsernameRefillPeriod());
            if (usernameDecision.retryAfterNanos() > 0) {
                return new Admission(false, toRetryAfterSeconds(usernameDecision.retryAfterNanos()));
            }
            Decision pairDecision = consume(pairBuckets, pairKey, properties.getPairCapacity(), properties.getPairRefillPeriod());
            if (pairDecision.retryAfterNanos() > 0) {
                return new Admission(false, toRetryAfterSeconds(pairDecision.retryAfterNanos()));
            }
            return Admission.permitted();
        } catch (LimiterFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LimiterFailureException();
        }
    }

    public void resetAfterSuccessfulLogin(String remoteAddress, String username) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String addressKey = canonicalAddress(remoteAddress);
            String usernameKey = canonicalUsername(username);
            usernameBuckets.invalidate(usernameKey);
            pairBuckets.invalidate(addressKey + "\u0000" + usernameKey);
        } catch (RuntimeException e) {
            throw new LimiterFailureException();
        }
    }

    long ipCacheSize() {
        return ipBuckets.estimatedSize();
    }

    long usernameCacheSize() {
        return usernameBuckets.estimatedSize();
    }

    long pairCacheSize() {
        return pairBuckets.estimatedSize();
    }

    void cleanUp() {
        ipBuckets.cleanUp();
        usernameBuckets.cleanUp();
        pairBuckets.cleanUp();
    }

    static String canonicalUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String canonicalAddress(String remoteAddress) {
        return remoteAddress == null || remoteAddress.isBlank() ? UNKNOWN_ADDRESS : remoteAddress;
    }

    private Decision consume(Cache<String, BucketState> cache, String key, int capacity, Duration refillPeriod) {
        long now = ticker.read();
        AtomicReference<Decision> decision = new AtomicReference<>();
        long refillNanos = refillPeriod.toNanos();
        cache.asMap().compute(key, (ignored, existing) -> {
            BucketState state = refill(existing, capacity, refillNanos, now);
            if (state.tokens() > 0) {
                decision.set(new Decision(0));
                return new BucketState(state.tokens() - 1, state.lastRefillNanos());
            }
            long elapsed = Math.max(0, now - state.lastRefillNanos());
            long wait = Math.max(1, refillNanos - elapsed);
            decision.set(new Decision(wait));
            return state;
        });
        return decision.get();
    }

    private static BucketState refill(BucketState state, int capacity, long refillNanos, long now) {
        if (state == null) {
            return new BucketState(capacity, now);
        }
        long elapsed = now - state.lastRefillNanos();
        if (elapsed <= 0 || refillNanos <= 0) {
            return state;
        }
        long periods = elapsed / refillNanos;
        if (periods == 0) {
            return state;
        }
        long available = Math.min(capacity, state.tokens() + periods);
        long lastRefill = available == capacity ? now : state.lastRefillNanos() + periods * refillNanos;
        return new BucketState((int) available, lastRefill);
    }

    private long toRetryAfterSeconds(long retryAfterNanos) {
        long seconds = (retryAfterNanos + 999_999_999L) / 1_000_000_000L;
        long maximumSeconds = Math.max(1, properties.getMaxRetryAfter().toSeconds());
        return Math.max(1, Math.min(seconds, maximumSeconds));
    }

    private static Cache<String, BucketState> createCache(LoginRateLimitProperties properties, Ticker ticker) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaximumSize())
                .expireAfterAccess(properties.getCacheExpireAfterAccess())
                .ticker(ticker)
                .build();
    }

    private record BucketState(int tokens, long lastRefillNanos) {
    }

    private record Decision(long retryAfterNanos) {
    }

    public record Admission(boolean allowed, long retryAfterSeconds) {
        public static Admission permitted() {
            return new Admission(true, 0);
        }
    }

    public static final class ThrottledException extends RuntimeException {
        private final long retryAfterSeconds;

        public ThrottledException(long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public static final class LimiterFailureException extends RuntimeException {
    }
}
