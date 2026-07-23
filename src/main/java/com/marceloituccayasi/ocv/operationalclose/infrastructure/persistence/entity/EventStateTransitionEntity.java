package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an immutable Operational Event state transition.
 *
 * <p>The event identifier is stored as a scalar UUID to avoid accidental
 * JPA cascades between the event and its append-only history.
 */
@Entity
@Table(name = "event_state_transition", schema = "ocv")
public class EventStateTransitionEntity {

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
            name = "from_state",
            updatable = false,
            length = 30)
    private String fromState;

    @Column(
            name = "to_state",
            nullable = false,
            updatable = false,
            length = 30)
    private String toState;

    @Column(
            name = "cause_code",
            nullable = false,
            updatable = false,
            length = 40)
    private String causeCode;

    @Column(
            name = "detail",
            updatable = false,
            columnDefinition = "TEXT")
    private String detail;

    @Column(
            name = "validation_result_id",
            updatable = false)
    private UUID validationResultId;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false)
    private Instant occurredAt;

    @Column(
            name = "actor_user_id",
            nullable = false,
            updatable = false,
            length = 64)
    private String actorUserId;

    @Column(
            name = "actor_username",
            nullable = false,
            updatable = false,
            length = 100)
    private String actorUsername;

    protected EventStateTransitionEntity() {
        // Required by JPA.
    }

    private EventStateTransitionEntity(
            UUID id,
            UUID eventId,
            String fromState,
            String toState,
            String causeCode,
            String detail,
            UUID validationResultId,
            Instant occurredAt,
            String actorUserId,
            String actorUsername) {

        this.id =
                Objects.requireNonNull(id);

        this.eventId =
                Objects.requireNonNull(eventId);

        this.fromState =
                fromState;

        this.toState =
                Objects.requireNonNull(toState);

        this.causeCode =
                Objects.requireNonNull(causeCode);

        this.detail =
                detail;

        this.validationResultId =
                validationResultId;

        this.occurredAt =
                Objects.requireNonNull(occurredAt);

        this.actorUserId =
                Objects.requireNonNull(actorUserId);

        this.actorUsername =
                Objects.requireNonNull(actorUsername);
    }

    public static EventStateTransitionEntity create(
            UUID id,
            UUID eventId,
            String fromState,
            String toState,
            String causeCode,
            String detail,
            UUID validationResultId,
            Instant occurredAt,
            String actorUserId,
            String actorUsername) {

        return new EventStateTransitionEntity(
                id,
                eventId,
                fromState,
                toState,
                causeCode,
                detail,
                validationResultId,
                occurredAt,
                actorUserId,
                actorUsername);
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public String fromState() {
        return fromState;
    }

    public String toState() {
        return toState;
    }

    public String causeCode() {
        return causeCode;
    }

    public String detail() {
        return detail;
    }

    public UUID validationResultId() {
        return validationResultId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String actorUserId() {
        return actorUserId;
    }

    public String actorUsername() {
        return actorUsername;
    }

}