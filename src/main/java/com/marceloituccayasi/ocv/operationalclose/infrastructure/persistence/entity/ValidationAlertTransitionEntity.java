package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an append-only Validation Alert transition.
 */
@Entity
@Table(name = "alert_transition", schema = "ocv")
public class ValidationAlertTransitionEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "alert_id",
            nullable = false,
            updatable = false)
    private UUID alertId;

    @Column(
            name = "from_state",
            updatable = false,
            length = 20)
    private String fromState;

    @Column(
            name = "to_state",
            nullable = false,
            updatable = false,
            length = 20)
    private String toState;

    @Column(
            name = "action_code",
            nullable = false,
            updatable = false,
            length = 40)
    private String actionCode;

    @Column(
            name = "detail",
            updatable = false)
    private String detail;

    @Column(
            name = "justification",
            updatable = false)
    private String justification;

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

    protected ValidationAlertTransitionEntity() {
        // Required by JPA.
    }

    private ValidationAlertTransitionEntity(
            UUID id,
            UUID alertId,
            String fromState,
            String toState,
            String actionCode,
            String detail,
            String justification,
            UUID validationResultId,
            Instant occurredAt,
            String actorUserId,
            String actorUsername) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.alertId =
                Objects.requireNonNull(
                        alertId);

        this.fromState =
                fromState;

        this.toState =
                Objects.requireNonNull(
                        toState);

        this.actionCode =
                Objects.requireNonNull(
                        actionCode);

        this.detail =
                detail;

        this.justification =
                justification;

        this.validationResultId =
                validationResultId;

        this.occurredAt =
                Objects.requireNonNull(
                        occurredAt);

        this.actorUserId =
                Objects.requireNonNull(
                        actorUserId);

        this.actorUsername =
                Objects.requireNonNull(
                        actorUsername);
    }

    public static ValidationAlertTransitionEntity create(
            UUID id,
            UUID alertId,
            String fromState,
            String toState,
            String actionCode,
            String detail,
            String justification,
            UUID validationResultId,
            Instant occurredAt,
            String actorUserId,
            String actorUsername) {

        return new ValidationAlertTransitionEntity(
                id,
                alertId,
                fromState,
                toState,
                actionCode,
                detail,
                justification,
                validationResultId,
                occurredAt,
                actorUserId,
                actorUsername);
    }

    public UUID id() {
        return id;
    }

    public UUID alertId() {
        return alertId;
    }

    public String fromState() {
        return fromState;
    }

    public String toState() {
        return toState;
    }

    public String actionCode() {
        return actionCode;
    }

    public String detail() {
        return detail;
    }

    public String justification() {
        return justification;
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