package com.bankingsystem.core.modules.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "auth.login.rate-limit")
public class LoginRateLimitProperties {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(60);

    private boolean enabled = true;
    private int ipCapacity = 20;
    private Duration ipRefillPeriod = Duration.ofSeconds(30);
    private int usernameCapacity = 10;
    private Duration usernameRefillPeriod = Duration.ofSeconds(60);
    private int pairCapacity = 5;
    private Duration pairRefillPeriod = Duration.ofSeconds(60);
    private int cacheMaximumSize = MAX_CACHE_SIZE;
    private Duration cacheExpireAfterAccess = Duration.ofMinutes(15);
    private Duration maxRetryAfter = MAX_RETRY_AFTER;

    @PostConstruct
    public void validateConfiguration() {
        validate();
    }

    public void validate() {
        requirePositive("ip-capacity", ipCapacity);
        requirePositive("username-capacity", usernameCapacity);
        requirePositive("pair-capacity", pairCapacity);
        requirePositive("cache-maximum-size", cacheMaximumSize);
        if (cacheMaximumSize > MAX_CACHE_SIZE) {
            throw new IllegalArgumentException("cache-maximum-size must be at most " + MAX_CACHE_SIZE);
        }
        requirePositive("ip-refill-period", ipRefillPeriod);
        requirePositive("username-refill-period", usernameRefillPeriod);
        requirePositive("pair-refill-period", pairRefillPeriod);
        requirePositive("cache-expire-after-access", cacheExpireAfterAccess);
        requirePositive("max-retry-after", maxRetryAfter);
        if (maxRetryAfter.compareTo(MAX_RETRY_AFTER) > 0) {
            throw new IllegalArgumentException("max-retry-after must be at most 60s");
        }
    }

    private static void requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requirePositive(String name, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        try {
            if (value.toNanos() <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " is too large", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIpCapacity() {
        return ipCapacity;
    }

    public void setIpCapacity(int ipCapacity) {
        this.ipCapacity = ipCapacity;
    }

    public Duration getIpRefillPeriod() {
        return ipRefillPeriod;
    }

    public void setIpRefillPeriod(Duration ipRefillPeriod) {
        this.ipRefillPeriod = ipRefillPeriod;
    }

    public int getUsernameCapacity() {
        return usernameCapacity;
    }

    public void setUsernameCapacity(int usernameCapacity) {
        this.usernameCapacity = usernameCapacity;
    }

    public Duration getUsernameRefillPeriod() {
        return usernameRefillPeriod;
    }

    public void setUsernameRefillPeriod(Duration usernameRefillPeriod) {
        this.usernameRefillPeriod = usernameRefillPeriod;
    }

    public int getPairCapacity() {
        return pairCapacity;
    }

    public void setPairCapacity(int pairCapacity) {
        this.pairCapacity = pairCapacity;
    }

    public Duration getPairRefillPeriod() {
        return pairRefillPeriod;
    }

    public void setPairRefillPeriod(Duration pairRefillPeriod) {
        this.pairRefillPeriod = pairRefillPeriod;
    }

    public int getCacheMaximumSize() {
        return cacheMaximumSize;
    }

    public void setCacheMaximumSize(int cacheMaximumSize) {
        this.cacheMaximumSize = cacheMaximumSize;
    }

    public Duration getCacheExpireAfterAccess() {
        return cacheExpireAfterAccess;
    }

    public void setCacheExpireAfterAccess(Duration cacheExpireAfterAccess) {
        this.cacheExpireAfterAccess = cacheExpireAfterAccess;
    }

    public Duration getMaxRetryAfter() {
        return maxRetryAfter;
    }

    public void setMaxRetryAfter(Duration maxRetryAfter) {
        this.maxRetryAfter = maxRetryAfter;
    }
}
