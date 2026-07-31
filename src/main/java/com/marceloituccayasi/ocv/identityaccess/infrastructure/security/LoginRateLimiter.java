package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration.LoginRateLimitProperties;

/**
 * Bounded in-memory login failure limiter.
 */
@Component
public final class LoginRateLimiter {

    private static final int DEFAULT_MAXIMUM_FAILURES = 10;
    private static final Duration DEFAULT_FAILURE_WINDOW =
            Duration.ofMinutes(5);
    private static final Duration DEFAULT_BLOCK_DURATION =
            Duration.ofMinutes(5);

    private static final int MAXIMUM_KEYS =
            10_000;

    private final Clock clock;
    private final int maximumFailures;
    private final Duration failureWindow;
    private final Duration blockDuration;

    private final Map<LoginRateLimitKey, AttemptState>
            attempts =
            new LinkedHashMap<>(
                    16,
                    0.75F,
                    true);

    public LoginRateLimiter() {
        this(Clock.systemUTC());
    }

    @Autowired
    public LoginRateLimiter(
            LoginRateLimitProperties properties) {

        this(
                Clock.systemUTC(),
                properties.maxFailures(),
                Duration.ofSeconds(
                        properties.windowSeconds()),
                Duration.ofSeconds(
                        properties.blockSeconds()));
    }

    LoginRateLimiter(Clock clock) {
        this(
                clock,
                DEFAULT_MAXIMUM_FAILURES,
                DEFAULT_FAILURE_WINDOW,
                DEFAULT_BLOCK_DURATION);
    }

    LoginRateLimiter(
            Clock clock,
            int maximumFailures,
            Duration failureWindow,
            Duration blockDuration) {

        this.clock =
                Objects.requireNonNull(clock);
        this.maximumFailures =
                maximumFailures;
        this.failureWindow =
                Objects.requireNonNull(failureWindow);
        this.blockDuration =
                Objects.requireNonNull(blockDuration);
    }

    public synchronized boolean isBlocked(
            LoginRateLimitKey key) {

        Objects.requireNonNull(key);

        Instant now = clock.instant();
        AttemptState state = attempts.get(key);

        if (state == null) {
            return false;
        }

        if (state.blockedUntil != null) {
            if (now.isBefore(state.blockedUntil)) {
                return true;
            }

            attempts.remove(key);
            return false;
        }

        pruneFailures(state, now);

        if (state.failures.isEmpty()) {
            attempts.remove(key);
        }

        return false;
    }

    /**
     * Records a failed authentication.
     *
     * @return true when the key is currently blocked
     */
    public synchronized boolean recordFailure(
            LoginRateLimitKey key) {

        Objects.requireNonNull(key);

        Instant now = clock.instant();
        AttemptState state =
                attempts.computeIfAbsent(
                        key,
                        ignored -> new AttemptState());

        if (state.blockedUntil != null) {
            if (now.isBefore(state.blockedUntil)) {
                return true;
            }

            state.reset();
        }

        pruneFailures(state, now);
        state.failures.addLast(now);

        if (state.failures.size()
                >= maximumFailures) {

            state.failures.clear();
            state.blockedUntil =
                    now.plus(blockDuration);
            enforceMaximumSize();
            return true;
        }

        enforceMaximumSize();
        return false;
    }

    public synchronized void recordSuccess(
            LoginRateLimitKey key) {

        attempts.remove(
                Objects.requireNonNull(key));
    }

    synchronized void clearAll() {
        attempts.clear();
    }

    private void pruneFailures(
            AttemptState state,
            Instant now) {

        Instant windowStart =
                now.minus(failureWindow);

        while (!state.failures.isEmpty()
                && !state.failures
                        .getFirst()
                        .isAfter(windowStart)) {

            state.failures.removeFirst();
        }
    }

    private void enforceMaximumSize() {
        while (attempts.size() > MAXIMUM_KEYS) {
            Iterator<LoginRateLimitKey> iterator =
                    attempts.keySet().iterator();

            if (!iterator.hasNext()) {
                return;
            }

            iterator.next();
            iterator.remove();
        }
    }

    private static final class AttemptState {

        private final Deque<Instant> failures =
                new ArrayDeque<>();

        private Instant blockedUntil;

        private void reset() {
            failures.clear();
            blockedUntil = null;
        }

    }

}