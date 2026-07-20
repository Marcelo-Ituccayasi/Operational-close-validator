package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private static final LoginRateLimitKey KEY =
            new LoginRateLimitKey(
                    "198.51.100.20",
                    "responsible");

    @Test
    void blocksAfterTenFailuresWithinFiveMinutes() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter =
                new LoginRateLimiter(clock);

        for (int attempt = 1;
                attempt < 10;
                attempt++) {

            assertThat(limiter.recordFailure(KEY))
                    .isFalse();

            assertThat(limiter.isBlocked(KEY))
                    .isFalse();
        }

        assertThat(limiter.recordFailure(KEY))
                .isTrue();

        assertThat(limiter.isBlocked(KEY))
                .isTrue();
    }

    @Test
    void blockedAttemptsDoNotExtendTheBlock() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter =
                new LoginRateLimiter(clock);

        block(limiter, KEY);

        clock.advance(Duration.ofMinutes(4));

        assertThat(limiter.recordFailure(KEY))
                .isTrue();

        clock.advance(Duration.ofMinutes(1));

        assertThat(limiter.isBlocked(KEY))
                .isFalse();
    }

    @Test
    void successfulLoginClearsPreviousFailures() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter =
                new LoginRateLimiter(clock);

        for (int attempt = 0;
                attempt < 9;
                attempt++) {

            limiter.recordFailure(KEY);
        }

        limiter.recordSuccess(KEY);

        assertThat(limiter.recordFailure(KEY))
                .isFalse();

        assertThat(limiter.isBlocked(KEY))
                .isFalse();
    }

    @Test
    void separatesAddressAndUsernameKeys() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter =
                new LoginRateLimiter(clock);

        block(limiter, KEY);

        LoginRateLimitKey otherAddress =
                new LoginRateLimitKey(
                        "198.51.100.21",
                        "responsible");

        LoginRateLimitKey otherUsername =
                new LoginRateLimitKey(
                        "198.51.100.20",
                        "another-user");

        assertThat(limiter.isBlocked(otherAddress))
                .isFalse();

        assertThat(limiter.isBlocked(otherUsername))
                .isFalse();
    }

    @Test
    void discardsFailuresOutsideTheWindow() {
        MutableClock clock = new MutableClock();
        LoginRateLimiter limiter =
                new LoginRateLimiter(clock);

        for (int attempt = 0;
                attempt < 9;
                attempt++) {

            limiter.recordFailure(KEY);
        }

        clock.advance(
                Duration.ofMinutes(5)
                        .plusSeconds(1));

        assertThat(limiter.recordFailure(KEY))
                .isFalse();

        assertThat(limiter.isBlocked(KEY))
                .isFalse();
    }

    private static void block(
            LoginRateLimiter limiter,
            LoginRateLimitKey key) {

        for (int attempt = 0;
                attempt < 10;
                attempt++) {

            limiter.recordFailure(key);
        }
    }

    private static final class MutableClock
            extends Clock {

        private Instant current =
                Instant.parse(
                        "2026-07-19T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException(
                        "Only UTC is supported.");
            }

            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

    }

}
