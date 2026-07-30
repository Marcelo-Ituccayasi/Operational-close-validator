package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an immutable accounting submission attempt.
 */
@Entity
@Table(
        name = "accounting_submission_attempt",
        schema = "ocv")
public class AccountingSubmissionAttemptEntity {

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
            name = "vr008_result_id",
            nullable = false,
            updatable = false)
    private UUID vr008ResultId;

    @Column(
            name = "consolidation_id",
            updatable = false)
    private UUID consolidationId;

    @Column(
            name = "outcome",
            nullable = false,
            updatable = false,
            length = 15)
    private String outcome;

    @Column(
            name = "attempted_at",
            nullable = false,
            updatable = false)
    private Instant attemptedAt;

    @Column(
            name = "attempted_by_user_id",
            nullable = false,
            updatable = false,
            length = 64)
    private String attemptedByUserId;

    @Column(
            name = "attempted_by_username",
            nullable = false,
            updatable = false,
            length = 100)
    private String attemptedByUsername;

    @Column(
            name = "summary",
            updatable = false,
            columnDefinition = "TEXT")
    private String summary;

    protected AccountingSubmissionAttemptEntity() {
        // Required by JPA.
    }

    private AccountingSubmissionAttemptEntity(
            UUID id,
            UUID closeId,
            UUID vr008ResultId,
            UUID consolidationId,
            String outcome,
            Instant attemptedAt,
            String attemptedByUserId,
            String attemptedByUsername,
            String summary) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.closeId =
                Objects.requireNonNull(
                        closeId);

        this.vr008ResultId =
                Objects.requireNonNull(
                        vr008ResultId);

        this.consolidationId =
                consolidationId;

        this.outcome =
                Objects.requireNonNull(
                        outcome);

        this.attemptedAt =
                Objects.requireNonNull(
                        attemptedAt);

        this.attemptedByUserId =
                Objects.requireNonNull(
                        attemptedByUserId);

        this.attemptedByUsername =
                Objects.requireNonNull(
                        attemptedByUsername);

        this.summary =
                summary;
    }

    public static AccountingSubmissionAttemptEntity create(
            UUID id,
            UUID closeId,
            UUID vr008ResultId,
            UUID consolidationId,
            String outcome,
            Instant attemptedAt,
            String attemptedByUserId,
            String attemptedByUsername,
            String summary) {

        return new AccountingSubmissionAttemptEntity(
                id,
                closeId,
                vr008ResultId,
                consolidationId,
                outcome,
                attemptedAt,
                attemptedByUserId,
                attemptedByUsername,
                summary);
    }

    public UUID id() {
        return id;
    }

    public UUID closeId() {
        return closeId;
    }

    public UUID vr008ResultId() {
        return vr008ResultId;
    }

    public UUID consolidationId() {
        return consolidationId;
    }

    public String outcome() {
        return outcome;
    }

    public Instant attemptedAt() {
        return attemptedAt;
    }

    public String attemptedByUserId() {
        return attemptedByUserId;
    }

    public String attemptedByUsername() {
        return attemptedByUsername;
    }

    public String summary() {
        return summary;
    }

}