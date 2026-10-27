package com.accommodationfinder.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now.get()); }
    };
    private final RateLimiter limiter = new RateLimiter(true, clock);

    @Test
    void allowsUpToTheLimitThenRefusesWithARetryHint() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("ip", 3, Duration.ofMinutes(1)).allowed()).isTrue();
        }
        now.addAndGet(20_000);
        RateLimiter.Decision refused = limiter.tryAcquire("ip", 3, Duration.ofMinutes(1));

        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds()).isEqualTo(40);
    }

    @Test
    void theWindowResetsOnceItHasPassed() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("ip", 3, Duration.ofMinutes(1));
        }
        now.addAndGet(60_000);
        assertThat(limiter.tryAcquire("ip", 3, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @Test
    void keysAreCountedSeparately() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("attacker", 3, Duration.ofMinutes(1));
        }
        assertThat(limiter.tryAcquire("attacker", 3, Duration.ofMinutes(1)).allowed()).isFalse();
        assertThat(limiter.tryAcquire("someone-else", 3, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @Test
    void canBeSwitchedOff() {
        RateLimiter off = new RateLimiter(false, clock);
        for (int i = 0; i < 100; i++) {
            assertThat(off.tryAcquire("ip", 1, Duration.ofHours(1)).allowed()).isTrue();
        }
    }
}
