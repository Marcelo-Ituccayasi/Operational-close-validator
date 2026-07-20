package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an immutable Operational Close state transition.
 *
 * <p>The close identifier is stored as a scalar UUID to keep aggregate
 * persistence explicit and avoid accidental JPA cascades.
 */
@Entity
@Table(name = "close_state_transition", schema = "ocv")
public class CloseStateTransitionEntity {

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
            name = "consolidation_id",
            updatable = false)
    private UUID consolidationId;

    @Column(
            name = "submission_attempt_id",
            updatable = false)
    private UUID submissionAttemptId;

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

    protected CloseStateTransitionEntity() {
        // Required by JPA.
    }

    private CloseStateTransitionEntity(
            UUID id,
            UUID closeId,
            String fromState,
            String toState,
            String causeCode,
            String detail,
            UUID validationResultId,
            UUID consolidationId,
            UUID submissionAttemptId,
            Instant occurredAt,
            String actorUserId,
            String actorUsername) {

        this.id = Objects.requireNonNull(id);
        this.closeId = Objects.requireNonNull(closeId);
        this.fromState = fromState;
        this.toState = Objects.requireNonNull(toState);
        this.causeCode = Objects.requireNonNull(causeCode);
        this.detail = detail;
        this.validationResultId = validationResultId;
        this.consolidationId = consolidationId;
        this.submissionAttemptId = submissionAttemptId;
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.actorUserId = Objects.requireNonNull(actorUserId);
        this.actorUsername = Objects.requireNonNull(actorUsername);
    }

    public static CloseStateTransitionEntity create(
            UUID id,
            UUID closeId,
            String fromState,
            String toState,
            String causeCode,
            String detail,
            UUID validationResultId,
            UUID consolidationId,
            UUID submissionAttemptId,
            Instant occurredAt,
            String actorUserId,
            String actorUsername) {

        return new CloseStateTransitionEntity(
                id,
                closeId,
                fromState,
                toState,
                causeCode,
                detail,
                validationResultId,
                consolidationId,
                submissionAttemptId,
                occurredAt,
                actorUserId,
                actorUsername);
    }

    public UUID id() {
        return id;
    }

    public UUID closeId() {
        return closeId;
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

    public UUID consolidationId() {
        return consolidationId;
    }

    public UUID submissionAttemptId() {
        return submissionAttemptId;
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
