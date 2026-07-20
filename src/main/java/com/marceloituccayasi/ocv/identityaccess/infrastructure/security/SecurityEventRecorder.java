package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.SecurityEventEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.SecurityEventJpaRepository;

/**
 * Persists structured security events without credentials or session tokens.
 */
@Component
public class SecurityEventRecorder {

    private static final int MAXIMUM_DETAIL_LENGTH = 500;

    private final SecurityEventJpaRepository repository;

    public SecurityEventRecorder(
            SecurityEventJpaRepository repository) {

        this.repository = repository;
    }

    @Transactional
    public void recordKnownIdentity(
            SecurityEventType eventType,
            AuthenticatedIdentity identity,
            String detail) {

        Objects.requireNonNull(identity);

        record(
                eventType,
                identity.userId(),
                identity.username(),
                detail);
    }

    @Transactional
    public void recordKnownIdentity(
            SecurityEventType eventType,
            String userId,
            String username,
            String detail) {

        record(
                eventType,
                Objects.requireNonNull(userId),
                username,
                detail);
    }

    @Transactional
    public void recordPresentedUsername(
            SecurityEventType eventType,
            String presentedUsername,
            String detail) {

        record(
                eventType,
                null,
                presentedUsername,
                detail);
    }

    private void record(
            SecurityEventType eventType,
            String userId,
            String username,
            String detail) {

        SecurityEventEntity securityEvent =
                SecurityEventEntity.create(
                        userId,
                        normalizeUsername(username),
                        Objects.requireNonNull(eventType),
                        Instant.now(),
                        sanitizeDetail(detail));

        repository.save(securityEvent);
    }

    private static String normalizeUsername(String value) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim().toLowerCase(Locale.ROOT);

        return normalized.isEmpty() ? null : normalized;
    }

    private static String sanitizeDetail(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        if (sanitized.length() <= MAXIMUM_DETAIL_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(
                0,
                MAXIMUM_DETAIL_LENGTH);
    }

}
