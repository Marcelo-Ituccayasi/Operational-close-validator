package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA representation of an immutable submission rejection issue.
 */
@Entity
@Table(
        name = "submission_attempt_issue",
        schema = "ocv")
public class SubmissionAttemptIssueEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false)
    private UUID id;

    @Column(
            name = "submission_attempt_id",
            nullable = false,
            updatable = false)
    private UUID submissionAttemptId;

    @Column(
            name = "issue_type",
            nullable = false,
            updatable = false,
            length = 40)
    private String issueType;

    @Column(
            name = "event_id",
            updatable = false)
    private UUID eventId;

    @Column(
            name = "alert_id",
            updatable = false)
    private UUID alertId;

    @Column(
            name = "validation_result_id",
            updatable = false)
    private UUID validationResultId;

    @Column(
            name = "consolidation_id",
            updatable = false)
    private UUID consolidationId;

    @Column(
            name = "detail",
            nullable = false,
            updatable = false,
            columnDefinition = "TEXT")
    private String detail;

    protected SubmissionAttemptIssueEntity() {
        // Required by JPA.
    }

    private SubmissionAttemptIssueEntity(
            UUID id,
            UUID submissionAttemptId,
            String issueType,
            UUID eventId,
            UUID alertId,
            UUID validationResultId,
            UUID consolidationId,
            String detail) {

        this.id =
                Objects.requireNonNull(
                        id);

        this.submissionAttemptId =
                Objects.requireNonNull(
                        submissionAttemptId);

        this.issueType =
                Objects.requireNonNull(
                        issueType);

        this.eventId =
                eventId;

        this.alertId =
                alertId;

        this.validationResultId =
                validationResultId;

        this.consolidationId =
                consolidationId;

        this.detail =
                Objects.requireNonNull(
                        detail);
    }

    public static SubmissionAttemptIssueEntity create(
            UUID id,
            UUID submissionAttemptId,
            String issueType,
            UUID eventId,
            UUID alertId,
            UUID validationResultId,
            UUID consolidationId,
            String detail) {

        return new SubmissionAttemptIssueEntity(
                id,
                submissionAttemptId,
                issueType,
                eventId,
                alertId,
                validationResultId,
                consolidationId,
                detail);
    }

    public UUID id() {
        return id;
    }

    public UUID submissionAttemptId() {
        return submissionAttemptId;
    }

    public String issueType() {
        return issueType;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID alertId() {
        return alertId;
    }

    public UUID validationResultId() {
        return validationResultId;
    }

    public UUID consolidationId() {
        return consolidationId;
    }

    public String detail() {
        return detail;
    }

}