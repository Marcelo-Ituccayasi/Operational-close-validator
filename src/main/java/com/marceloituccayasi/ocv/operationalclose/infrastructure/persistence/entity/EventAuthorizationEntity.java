package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an Event Authorization.
 *
 * <p>This class is a persistence model and must not depend on domain objects.
 */
@Entity
@Table(name = "event_authorization", schema = "ocv")
public class EventAuthorizationEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false)
    private UUID eventId;

    @Column(
            name = "authorized_by_name",
            nullable = false,
            length = 200)
    private String authorizedByName;

    @Column(
            name = "reason",
            nullable = false,
            columnDefinition = "TEXT")
    private String reason;

    @Column(
            name = "authorized_at",
            nullable = false)
    private Instant authorizedAt;

    @Column(
            name = "formal_reference",
            nullable = false,
            length = 500)
    private String formalReference;

    @Column(
            name = "is_active",
            nullable = false)
    private boolean active;

    @Column(
            name = "revision",
            nullable = false)
    private long revision;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false)
    private Instant createdAt;

    @Column(
            name = "created_by_user_id",
            nullable = false,
            updatable = false,
            length = 64)
    private String createdByUserId;

    @Column(
            name = "created_by_username",
            nullable = false,
            updatable = false,
            length = 100)
    private String createdByUsername;

    @Column(
            name = "updated_at",
            nullable = false)
    private Instant updatedAt;

    @Column(
            name = "updated_by_user_id",
            nullable = false,
            length = 64)
    private String updatedByUserId;

    @Column(
            name = "updated_by_username",
            nullable = false,
            length = 100)
    private String updatedByUsername;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected EventAuthorizationEntity() {
        // Required by JPA.
    }

    private EventAuthorizationEntity(
            UUID id,
            UUID eventId,
            String authorizedByName,
            String reason,
            Instant authorizedAt,
            String formalReference,
            boolean active,
            long revision,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername,
            Instant deactivatedAt) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.eventId =
                Objects.requireNonNull(
                        eventId);

        this.authorizedByName =
                Objects.requireNonNull(
                        authorizedByName);

        this.reason =
                Objects.requireNonNull(
                        reason);

        this.authorizedAt =
                Objects.requireNonNull(
                        authorizedAt);

        this.formalReference =
                Objects.requireNonNull(
                        formalReference);

        this.active =
                active;

        this.revision =
                revision;

        this.createdAt =
                Objects.requireNonNull(
                        createdAt);

        this.createdByUserId =
                Objects.requireNonNull(
                        createdByUserId);

        this.createdByUsername =
                Objects.requireNonNull(
                        createdByUsername);

        this.updatedAt =
                Objects.requireNonNull(
                        updatedAt);

        this.updatedByUserId =
                Objects.requireNonNull(
                        updatedByUserId);

        this.updatedByUsername =
                Objects.requireNonNull(
                        updatedByUsername);

        this.deactivatedAt =
                deactivatedAt;
    }

    public static EventAuthorizationEntity create(
            UUID id,
            UUID eventId,
            String authorizedByName,
            String reason,
            Instant authorizedAt,
            String formalReference,
            boolean active,
            long revision,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername,
            Instant deactivatedAt) {

        return new EventAuthorizationEntity(
                id,
                eventId,
                authorizedByName,
                reason,
                authorizedAt,
                formalReference,
                active,
                revision,
                createdAt,
                createdByUserId,
                createdByUsername,
                updatedAt,
                updatedByUserId,
                updatedByUsername,
                deactivatedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public String authorizedByName() {
        return authorizedByName;
    }

    public String reason() {
        return reason;
    }

    public Instant authorizedAt() {
        return authorizedAt;
    }

    public String formalReference() {
        return formalReference;
    }

    public boolean active() {
        return active;
    }

    public long revision() {
        return revision;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String createdByUserId() {
        return createdByUserId;
    }

    public String createdByUsername() {
        return createdByUsername;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String updatedByUserId() {
        return updatedByUserId;
    }

    public String updatedByUsername() {
        return updatedByUsername;
    }

    public Instant deactivatedAt() {
        return deactivatedAt;
    }

}