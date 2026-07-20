package com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.SecurityEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only persistence representation of a security event.
 *
 * <p>The optional source address will be mapped when trusted proxy
 * resolution is introduced.
 */
@Entity
@Table(name = "security_event", schema = "ocv")
public class SecurityEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "username_normalized", length = 100)
    private String usernameNormalized;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private SecurityEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "detail", length = 500)
    private String detail;

    protected SecurityEventEntity() {
        // Required by JPA.
    }

    private SecurityEventEntity(
            UUID id,
            String userId,
            String usernameNormalized,
            SecurityEventType eventType,
            Instant occurredAt,
            String detail) {

        this.id = Objects.requireNonNull(id);
        this.userId = userId;
        this.usernameNormalized = usernameNormalized;
        this.eventType = Objects.requireNonNull(eventType);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.detail = detail;
    }

    public static SecurityEventEntity create(
            String userId,
            String usernameNormalized,
            SecurityEventType eventType,
            Instant occurredAt,
            String detail) {

        return new SecurityEventEntity(
                UUID.randomUUID(),
                userId,
                usernameNormalized,
                eventType,
                occurredAt,
                detail);
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String usernameNormalized() {
        return usernameNormalized;
    }

    public SecurityEventType eventType() {
        return eventType;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String detail() {
        return detail;
    }

}
