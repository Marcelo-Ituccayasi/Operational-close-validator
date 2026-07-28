package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of a persisted Validation Alert.
 *
 * <p>This persistence model supports both Event-scoped and Close-scoped
 * alerts without depending on domain objects.</p>
 */
@Entity
@Table(name = "alert", schema = "ocv")
public class ValidationAlertEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "event_id",
            updatable = false)
    private UUID eventId;

    @Column(
            name = "close_id",
            updatable = false)
    private UUID closeId;

    @Column(
            name = "source_validation_result_id",
            updatable = false)
    private UUID sourceValidationResultId;

    @Column(
            name = "cause_code",
            nullable = false,
            updatable = false,
            length = 40)
    private String causeCode;

    @Column(
            name = "severity",
            nullable = false,
            updatable = false,
            length = 10)
    private String severity;

    @Column(
            name = "is_blocking",
            nullable = false,
            updatable = false)
    private boolean blocking;

    @Column(
            name = "state",
            nullable = false,
            length = 20)
    private String state;

    @Column(
            name = "detail",
            nullable = false,
            updatable = false)
    private String detail;

    @Column(name = "resolved_by_validation_result_id")
    private UUID resolvedByValidationResultId;

    @Column(name = "discard_justification")
    private String discardJustification;

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

    @Column(name = "closed_at")
    private Instant closedAt;

    protected ValidationAlertEntity() {
        // Required by JPA.
    }

    private ValidationAlertEntity(
            UUID id,
            UUID eventId,
            UUID closeId,
            UUID sourceValidationResultId,
            String causeCode,
            String severity,
            boolean blocking,
            String state,
            String detail,
            UUID resolvedByValidationResultId,
            String discardJustification,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            Instant closedAt) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.eventId =
                eventId;

        this.closeId =
                closeId;

        this.sourceValidationResultId =
                sourceValidationResultId;

        this.causeCode =
                Objects.requireNonNull(
                        causeCode);

        this.severity =
                Objects.requireNonNull(
                        severity);

        this.blocking =
                blocking;

        this.state =
                Objects.requireNonNull(
                        state);

        this.detail =
                Objects.requireNonNull(
                        detail);

        this.resolvedByValidationResultId =
                resolvedByValidationResultId;

        this.discardJustification =
                discardJustification;

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

        this.closedAt =
                closedAt;
    }

    public static ValidationAlertEntity create(
            UUID id,
            UUID eventId,
            UUID closeId,
            UUID sourceValidationResultId,
            String causeCode,
            String severity,
            boolean blocking,
            String state,
            String detail,
            UUID resolvedByValidationResultId,
            String discardJustification,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            Instant updatedAt,
            Instant closedAt) {

        return new ValidationAlertEntity(
                id,
                eventId,
                closeId,
                sourceValidationResultId,
                causeCode,
                severity,
                blocking,
                state,
                detail,
                resolvedByValidationResultId,
                discardJustification,
                createdAt,
                createdByUserId,
                createdByUsername,
                updatedAt,
                closedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID closeId() {
        return closeId;
    }

    public UUID sourceValidationResultId() {
        return sourceValidationResultId;
    }

    public String causeCode() {
        return causeCode;
    }

    public String severity() {
        return severity;
    }

    public boolean blocking() {
        return blocking;
    }

    public String state() {
        return state;
    }

    public String detail() {
        return detail;
    }

    public UUID resolvedByValidationResultId() {
        return resolvedByValidationResultId;
    }

    public String discardJustification() {
        return discardJustification;
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

    public Instant closedAt() {
        return closedAt;
    }

}