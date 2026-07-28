package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of a persisted Validation Result.
 *
 * <p>This persistence model supports both Event-scoped and Close-scoped
 * results without depending on domain objects.</p>
 */
@Entity
@Table(name = "validation_result", schema = "ocv")
public class ValidationResultEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "rule_code",
            nullable = false,
            updatable = false,
            length = 10)
    private String ruleCode;

    @Column(
            name = "rule_version",
            nullable = false,
            updatable = false)
    private int ruleVersion;

    @Column(
            name = "event_id",
            updatable = false)
    private UUID eventId;

    @Column(
            name = "close_id",
            updatable = false)
    private UUID closeId;

    @Column(
            name = "outcome",
            nullable = false,
            updatable = false,
            length = 15)
    private String outcome;

    @Column(
            name = "detail",
            nullable = false,
            updatable = false)
    private String detail;

    @Column(
            name = "evaluated_at",
            nullable = false,
            updatable = false)
    private Instant evaluatedAt;

    @Column(
            name = "evaluated_by_user_id",
            nullable = false,
            updatable = false,
            length = 64)
    private String evaluatedByUserId;

    @Column(
            name = "evaluated_by_username",
            nullable = false,
            updatable = false,
            length = 100)
    private String evaluatedByUsername;

    @Column(
            name = "event_data_revision",
            updatable = false)
    private Long eventDataRevision;

    @Column(
            name = "consolidation_id",
            updatable = false)
    private UUID consolidationId;

    @Column(
            name = "is_current",
            nullable = false)
    private boolean current;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "invalidation_reason")
    private String invalidationReason;

    protected ValidationResultEntity() {
        // Required by JPA.
    }

    private ValidationResultEntity(
            UUID id,
            String ruleCode,
            int ruleVersion,
            UUID eventId,
            UUID closeId,
            String outcome,
            String detail,
            Instant evaluatedAt,
            String evaluatedByUserId,
            String evaluatedByUsername,
            Long eventDataRevision,
            UUID consolidationId,
            boolean current,
            Instant invalidatedAt,
            String invalidationReason) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.ruleCode =
                Objects.requireNonNull(
                        ruleCode);

        this.ruleVersion =
                ruleVersion;

        this.eventId =
                eventId;

        this.closeId =
                closeId;

        this.outcome =
                Objects.requireNonNull(
                        outcome);

        this.detail =
                Objects.requireNonNull(
                        detail);

        this.evaluatedAt =
                Objects.requireNonNull(
                        evaluatedAt);

        this.evaluatedByUserId =
                Objects.requireNonNull(
                        evaluatedByUserId);

        this.evaluatedByUsername =
                Objects.requireNonNull(
                        evaluatedByUsername);

        this.eventDataRevision =
                eventDataRevision;

        this.consolidationId =
                consolidationId;

        this.current =
                current;

        this.invalidatedAt =
                invalidatedAt;

        this.invalidationReason =
                invalidationReason;
    }

    public static ValidationResultEntity create(
            UUID id,
            String ruleCode,
            int ruleVersion,
            UUID eventId,
            UUID closeId,
            String outcome,
            String detail,
            Instant evaluatedAt,
            String evaluatedByUserId,
            String evaluatedByUsername,
            Long eventDataRevision,
            UUID consolidationId,
            boolean current,
            Instant invalidatedAt,
            String invalidationReason) {

        return new ValidationResultEntity(
                id,
                ruleCode,
                ruleVersion,
                eventId,
                closeId,
                outcome,
                detail,
                evaluatedAt,
                evaluatedByUserId,
                evaluatedByUsername,
                eventDataRevision,
                consolidationId,
                current,
                invalidatedAt,
                invalidationReason);
    }

    public UUID id() {
        return id;
    }

    public String ruleCode() {
        return ruleCode;
    }

    public int ruleVersion() {
        return ruleVersion;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID closeId() {
        return closeId;
    }

    public String outcome() {
        return outcome;
    }

    public String detail() {
        return detail;
    }

    public Instant evaluatedAt() {
        return evaluatedAt;
    }

    public String evaluatedByUserId() {
        return evaluatedByUserId;
    }

    public String evaluatedByUsername() {
        return evaluatedByUsername;
    }

    public Long eventDataRevision() {
        return eventDataRevision;
    }

    public UUID consolidationId() {
        return consolidationId;
    }

    public boolean current() {
        return current;
    }

    public Instant invalidatedAt() {
        return invalidatedAt;
    }

    public String invalidationReason() {
        return invalidationReason;
    }

}