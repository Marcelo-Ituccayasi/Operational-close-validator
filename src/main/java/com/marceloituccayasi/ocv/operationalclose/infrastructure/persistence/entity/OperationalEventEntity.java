package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an Operational Event.
 *
 * <p>This class is a persistence model and must not depend on domain objects.
 */
@Entity
@Table(name = "operational_event", schema = "ocv")
public class OperationalEventEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "close_id",
            nullable = false,
            updatable = false)
    private UUID closeId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 20)
    private String eventType;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 4)
    private BigDecimal amount;

    @Column(
            name = "balance_effect",
            nullable = false,
            precision = 19,
            scale = 4)
    private BigDecimal balanceEffect;

    @Column(
            name = "reversed_event_id")
    private UUID reversedEventId;

    @Column(
            name = "occurred_at",
            nullable = false)
    private Instant occurredAt;

    @Column(
            name = "registered_at",
            nullable = false,
            updatable = false)
    private Instant registeredAt;

    @Column(
            name = "responsible_name",
            nullable = false,
            length = 200)
    private String responsibleName;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT")
    private String description;

    @Column(
            name = "state",
            nullable = false,
            length = 30)
    private String state;

    @Column(
            name = "evidence_required",
            nullable = false)
    private boolean evidenceRequired;

    @Column(
            name = "authorization_required",
            nullable = false)
    private boolean authorizationRequired;

    @Column(
            name = "data_revision",
            nullable = false)
    private long dataRevision;

    @Column(
            name = "state_changed_at",
            nullable = false)
    private Instant stateChangedAt;

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

    protected OperationalEventEntity() {
        // Required by JPA.
    }

    private OperationalEventEntity(
            UUID id,
            UUID closeId,
            String eventType,
            BigDecimal amount,
            BigDecimal balanceEffect,
            UUID reversedEventId,
            Instant occurredAt,
            Instant registeredAt,
            String responsibleName,
            String description,
            String state,
            boolean evidenceRequired,
            boolean authorizationRequired,
            long dataRevision,
            Instant stateChangedAt,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername) {

        this.id =
                Objects.requireNonNull(id);

        this.closeId =
                Objects.requireNonNull(closeId);

        this.eventType =
                Objects.requireNonNull(eventType);

        this.amount =
                Objects.requireNonNull(amount);

        this.balanceEffect =
                Objects.requireNonNull(balanceEffect);

        this.reversedEventId =
                reversedEventId;

        this.occurredAt =
                Objects.requireNonNull(occurredAt);

        this.registeredAt =
                Objects.requireNonNull(registeredAt);

        this.responsibleName =
                Objects.requireNonNull(responsibleName);

        this.description =
                Objects.requireNonNull(description);

        this.state =
                Objects.requireNonNull(state);

        this.evidenceRequired =
                evidenceRequired;

        this.authorizationRequired =
                authorizationRequired;

        this.dataRevision =
                dataRevision;

        this.stateChangedAt =
                Objects.requireNonNull(stateChangedAt);

        this.createdAt =
                Objects.requireNonNull(createdAt);

        this.createdByUserId =
                Objects.requireNonNull(createdByUserId);

        this.createdByUsername =
                Objects.requireNonNull(createdByUsername);

        this.updatedAt =
                Objects.requireNonNull(updatedAt);

        this.updatedByUserId =
                Objects.requireNonNull(updatedByUserId);

        this.updatedByUsername =
                Objects.requireNonNull(updatedByUsername);
    }

    public static OperationalEventEntity create(
            UUID id,
            UUID closeId,
            String eventType,
            BigDecimal amount,
            BigDecimal balanceEffect,
            UUID reversedEventId,
            Instant occurredAt,
            Instant registeredAt,
            String responsibleName,
            String description,
            String state,
            boolean evidenceRequired,
            boolean authorizationRequired,
            long dataRevision,
            Instant stateChangedAt,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            String updatedByUserId,
            String updatedByUsername) {

        return new OperationalEventEntity(
                id,
                closeId,
                eventType,
                amount,
                balanceEffect,
                reversedEventId,
                occurredAt,
                registeredAt,
                responsibleName,
                description,
                state,
                evidenceRequired,
                authorizationRequired,
                dataRevision,
                stateChangedAt,
                createdAt,
                createdByUserId,
                createdByUsername,
                updatedAt,
                updatedByUserId,
                updatedByUsername);
    }

    public UUID id() {
        return id;
    }

    public UUID closeId() {
        return closeId;
    }

    public String eventType() {
        return eventType;
    }

    public BigDecimal amount() {
        return amount;
    }

    public BigDecimal balanceEffect() {
        return balanceEffect;
    }

    public UUID reversedEventId() {
        return reversedEventId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public String responsibleName() {
        return responsibleName;
    }

    public String description() {
        return description;
    }

    public String state() {
        return state;
    }

    public boolean evidenceRequired() {
        return evidenceRequired;
    }

    public boolean authorizationRequired() {
        return authorizationRequired;
    }

    public long dataRevision() {
        return dataRevision;
    }

    public Instant stateChangedAt() {
        return stateChangedAt;
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

}