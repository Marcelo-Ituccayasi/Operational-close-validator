package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssue;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.AccountingSubmissionAttemptEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SubmissionAttemptIssueEntity;

/**
 * Explicit mapping between accounting submission domain objects and JPA
 * entities.
 */
@Component
public final class AccountingSubmissionAttemptPersistenceMapper {

    public AccountingSubmissionAttemptEntity toEntity(
            AccountingSubmissionAttempt attempt) {

        Objects.requireNonNull(
                attempt,
                "attempt must not be null");

        return AccountingSubmissionAttemptEntity.create(
                attempt.id().value(),
                attempt.closeId().value(),
                attempt.vr008ResultId().value(),
                attempt.consolidationId() == null
                        ? null
                        : attempt.consolidationId().value(),
                attempt.outcome().name(),
                attempt.attemptedAt(),
                attempt.attemptedBy().userId(),
                attempt.attemptedBy().username(),
                attempt.summary());
    }

    public List<SubmissionAttemptIssueEntity> toIssueEntities(
            AccountingSubmissionAttempt attempt) {

        Objects.requireNonNull(
                attempt,
                "attempt must not be null");

        return attempt.issues()
                .stream()
                .map(
                        this::toIssueEntity)
                .toList();
    }

    public SubmissionAttemptIssueEntity toIssueEntity(
            SubmissionAttemptIssue issue) {

        Objects.requireNonNull(
                issue,
                "issue must not be null");

        return SubmissionAttemptIssueEntity.create(
                issue.id().value(),
                issue.submissionAttemptId().value(),
                issue.issueType().name(),
                issue.eventId() == null
                        ? null
                        : issue.eventId().value(),
                issue.alertId() == null
                        ? null
                        : issue.alertId().value(),
                issue.validationResultId() == null
                        ? null
                        : issue.validationResultId().value(),
                issue.consolidationId() == null
                        ? null
                        : issue.consolidationId().value(),
                issue.detail());
    }

    public AccountingSubmissionAttempt toDomain(
            AccountingSubmissionAttemptEntity entity,
            List<SubmissionAttemptIssueEntity> issueEntities) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        Objects.requireNonNull(
                issueEntities,
                "issueEntities must not be null");

        List<SubmissionAttemptIssue> issues =
                issueEntities.stream()
                        .map(
                                this::toIssueDomain)
                        .toList();

        return new AccountingSubmissionAttempt(
                new AccountingSubmissionAttemptId(
                        entity.id()),
                new OperationalCloseId(
                        entity.closeId()),
                new ValidationResultId(
                        entity.vr008ResultId()),
                entity.consolidationId() == null
                        ? null
                        : new ConsolidationId(
                                entity.consolidationId()),
                AccountingSubmissionOutcome.valueOf(
                        entity.outcome()),
                entity.attemptedAt(),
                new AuditActor(
                        entity.attemptedByUserId(),
                        entity.attemptedByUsername()),
                entity.summary(),
                issues);
    }

    public SubmissionAttemptIssue toIssueDomain(
            SubmissionAttemptIssueEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        return new SubmissionAttemptIssue(
                new SubmissionAttemptIssueId(
                        entity.id()),
                new AccountingSubmissionAttemptId(
                        entity.submissionAttemptId()),
                SubmissionAttemptIssueType.valueOf(
                        entity.issueType()),
                entity.eventId() == null
                        ? null
                        : new OperationalEventId(
                                entity.eventId()),
                entity.alertId() == null
                        ? null
                        : new ValidationAlertId(
                                entity.alertId()),
                entity.validationResultId() == null
                        ? null
                        : new ValidationResultId(
                                entity.validationResultId()),
                entity.consolidationId() == null
                        ? null
                        : new ConsolidationId(
                                entity.consolidationId()),
                entity.detail());
    }

}