package com.bankingsystem.core.features.auth.application;

import com.bankingsystem.core.modules.common.config.LoginRateLimitProperties;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    @Test
    void defaultsMatchApprovedPolicy() {
        LoginRateLimitProperties properties = new LoginRateLimitProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getIpCapacity()).isEqualTo(20);
        assertThat(properties.getIpRefillPeriod()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getUsernameCapacity()).isEqualTo(10);
        assertThat(properties.getUsernameRefillPeriod()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getPairCapacity()).isEqualTo(5);
        assertThat(properties.getPairRefillPeriod()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getCacheMaximumSize()).isEqualTo(10_000);
        assertThat(properties.getCacheExpireAfterAccess()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getMaxRetryAfter()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void exactCapacityAndRefillAreAppliedPerDimension() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(2);
        properties.setUsernameCapacity(100);
        properties.setPairCapacity(100);
        properties.setIpRefillPeriod(Duration.ofSeconds(30));
        properties.setUsernameRefillPeriod(Duration.ofSeconds(60));
        properties.setPairRefillPeriod(Duration.ofSeconds(60));
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        LoginRateLimiter.Admission rejected = limiter.tryAdmit("10.0.0.1", "alice");

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(30);

        ticker.advance(Duration.ofSeconds(30));

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
    }

    @Test
    void canonicalUsernameVariantsShareOneLimiterKey() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(100);
        properties.setUsernameCapacity(2);
        properties.setPairCapacity(100);
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        assertThat(limiter.tryAdmit("10.0.0.1", " Alice ").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "ALICE").allowed()).isFalse();
    }

    @Test
    void usernameRefillAndPairIsolationFollowConfiguredDimensions() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(100);
        properties.setUsernameCapacity(1);
        properties.setPairCapacity(1);
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.2", "alice").allowed()).isFalse();

        ticker.advance(Duration.ofSeconds(60));

        assertThat(limiter.tryAdmit("10.0.0.2", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "bob").allowed()).isTrue();
    }

    @Test
    void pairCapacityAndRefillAreIndependentFromOtherKeys() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(100);
        properties.setUsernameCapacity(100);
        properties.setPairCapacity(2);
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isFalse();

        ticker.advance(Duration.ofSeconds(60));

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
    }

    @Test
    void successfulLoginClearsUsernameAndPairButKeepsIpHistory() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(2);
        properties.setUsernameCapacity(1);
        properties.setPairCapacity(1);
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        limiter.resetAfterSuccessfulLogin("10.0.0.1", "alice");

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isFalse();
    }

    @Test
    void retryAfterIsCappedAndDisabledLimiterDoesNotCreateState() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(1);
        properties.setIpRefillPeriod(Duration.ofSeconds(120));
        properties.setUsernameCapacity(100);
        properties.setPairCapacity(100);
        properties.setMaxRetryAfter(Duration.ofSeconds(60));
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        assertThat(limiter.tryAdmit("10.0.0.1", "alice").allowed()).isTrue();
        assertThat(limiter.tryAdmit("10.0.0.1", "alice").retryAfterSeconds()).isEqualTo(60);

        properties.setEnabled(false);
        LoginRateLimiter disabled = new LoginRateLimiter(properties, ticker);
        assertThat(disabled.tryAdmit("10.0.0.2", "bob").allowed()).isTrue();
        assertThat(disabled.ipCacheSize()).isZero();
        assertThat(disabled.usernameCacheSize()).isZero();
        assertThat(disabled.pairCacheSize()).isZero();
    }

    @Test
    void cachesRemainBoundedAndIdleEntriesExpire() {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setCacheMaximumSize(3);
        properties.setCacheExpireAfterAccess(Duration.ofMinutes(15));
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);

        for (int i = 0; i < 20; i++) {
            limiter.tryAdmit("10.0.0." + i, "user-" + i);
        }
        limiter.cleanUp();

        assertThat(limiter.ipCacheSize()).isLessThanOrEqualTo(3);
        assertThat(limiter.usernameCacheSize()).isLessThanOrEqualTo(3);
        assertThat(limiter.pairCacheSize()).isLessThanOrEqualTo(3);

        ticker.advance(Duration.ofMinutes(16));
        limiter.cleanUp();

        assertThat(limiter.ipCacheSize()).isZero();
        assertThat(limiter.usernameCacheSize()).isZero();
        assertThat(limiter.pairCacheSize()).isZero();
    }

    @Test
    void concurrentAdmissionCannotExceedIpCapacity() throws Exception {
        ManualTicker ticker = new ManualTicker();
        LoginRateLimitProperties properties = properties();
        properties.setIpCapacity(20);
        properties.setUsernameCapacity(100);
        properties.setPairCapacity(100);
        LoginRateLimiter limiter = new LoginRateLimiter(properties, ticker);
        int attempts = 40;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CyclicBarrier barrier = new CyclicBarrier(attempts);
        try {
            List<Future<LoginRateLimiter.Admission>> results = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                int index = i;
                results.add(executor.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return limiter.tryAdmit("10.0.0.1", "user-" + index);
                }));
            }

            long allowed = 0;
            for (Future<LoginRateLimiter.Admission> result : results) {
                if (result.get(10, TimeUnit.SECONDS).allowed()) {
                    allowed++;
                }
            }
            assertThat(allowed).isEqualTo(20);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void invalidConfigurationIsRejected() {
        LoginRateLimitProperties properties = properties();

        properties.setIpCapacity(0);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);

        properties = properties();
        properties.setUsernameRefillPeriod(Duration.ZERO);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);

        properties = properties();
        properties.setCacheMaximumSize(10_001);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);

        properties = properties();
        properties.setCacheExpireAfterAccess(Duration.ZERO);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
    }

    private static LoginRateLimitProperties properties() {
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        properties.setEnabled(true);
        properties.setIpCapacity(20);
        properties.setIpRefillPeriod(Duration.ofSeconds(30));
        properties.setUsernameCapacity(10);
        properties.setUsernameRefillPeriod(Duration.ofSeconds(60));
        properties.setPairCapacity(5);
        properties.setPairRefillPeriod(Duration.ofSeconds(60));
        properties.setCacheMaximumSize(10_000);
        properties.setCacheExpireAfterAccess(Duration.ofMinutes(15));
        properties.setMaxRetryAfter(Duration.ofSeconds(60));
        return properties;
    }

    private static final class ManualTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
