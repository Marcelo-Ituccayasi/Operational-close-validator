package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * JPA representation of an immutable consolidation Event snapshot.
 */
@Entity
@IdClass(ConsolidationEventSnapshotEntityId.class)
@Table(
        name = "consolidation_event_snapshot",
        schema = "ocv")
public class ConsolidationEventSnapshotEntity {

    @Id
    @Column(
            name = "consolidation_id",
            nullable = false,
            updatable = false)
    private UUID consolidationId;

    @Id
    @Column(
            name = "event_id",
            nullable = false,
            updatable = false)
    private UUID eventId;

    @Column(
            name = "event_data_revision",
            nullable = false,
            updatable = false)
    private long eventDataRevision;

    @Column(
            name = "event_type",
            nullable = false,
            updatable = false,
            length = 20)
    private String eventType;

    @Column(
            name = "amount",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal amount;

    @Column(
            name = "balance_effect",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4)
    private BigDecimal balanceEffect;

    @Column(
            name = "reversed_event_id",
            updatable = false)
    private UUID reversedEventId;

    @Column(
            name = "event_state",
            nullable = false,
            updatable = false,
            length = 30)
    private String eventState;

    @Column(
            name = "captured_at",
            nullable = false,
            updatable = false)
    private Instant capturedAt;

    protected ConsolidationEventSnapshotEntity() {
        // Required by JPA.
    }

    private ConsolidationEventSnapshotEntity(
            UUID consolidationId,
            UUID eventId,
            long eventDataRevision,
            String eventType,
            BigDecimal amount,
            BigDecimal balanceEffect,
            UUID reversedEventId,
            String eventState,
            Instant capturedAt) {

        this.consolidationId =
                Objects.requireNonNull(
                        consolidationId);

        this.eventId =
                Objects.requireNonNull(
                        eventId);

        this.eventDataRevision =
                eventDataRevision;

        this.eventType =
                Objects.requireNonNull(
                        eventType);

        this.amount =
                Objects.requireNonNull(
                        amount);

        this.balanceEffect =
                Objects.requireNonNull(
                        balanceEffect);

        this.reversedEventId =
                reversedEventId;

        this.eventState =
                Objects.requireNonNull(
                        eventState);

        this.capturedAt =
                Objects.requireNonNull(
                        capturedAt);
    }

    public static ConsolidationEventSnapshotEntity create(
            UUID consolidationId,
            UUID eventId,
            long eventDataRevision,
            String eventType,
            BigDecimal amount,
            BigDecimal balanceEffect,
            UUID reversedEventId,
            String eventState,
            Instant capturedAt) {

        return new ConsolidationEventSnapshotEntity(
                consolidationId,
                eventId,
                eventDataRevision,
                eventType,
                amount,
                balanceEffect,
                reversedEventId,
                eventState,
                capturedAt);
    }

    public UUID consolidationId() {
        return consolidationId;
    }

    public UUID eventId() {
        return eventId;
    }

    public long eventDataRevision() {
        return eventDataRevision;
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

    public String eventState() {
        return eventState;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

}