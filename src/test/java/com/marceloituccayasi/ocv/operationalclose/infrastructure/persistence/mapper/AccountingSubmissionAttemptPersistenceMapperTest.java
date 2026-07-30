package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssue;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueId;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.AccountingSubmissionAttemptEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SubmissionAttemptIssueEntity;

class AccountingSubmissionAttemptPersistenceMapperTest {

    private static final AccountingSubmissionAttemptId ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "a4000000-0000-0000-0000-000000000001"));

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "a4000000-0000-0000-0000-000000000002"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "a4000000-0000-0000-0000-000000000003"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "a4000000-0000-0000-0000-000000000004"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    uuid(
                            "a4000000-0000-0000-0000-000000000005"));

    private static final Instant ATTEMPTED_AT =
            Instant.parse(
                    "2026-07-30T15:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final AccountingSubmissionAttemptPersistenceMapper mapper =
            new AccountingSubmissionAttemptPersistenceMapper();

    @Test
    void mapsAndReconstructsSuccessfulAttempt() {
        AccountingSubmissionAttempt expected =
                AccountingSubmissionAttempt.succeeded(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Close sent to accounting.");

        AccountingSubmissionAttemptEntity entity =
                mapper.toEntity(
                        expected);

        AccountingSubmissionAttempt reconstructed =
                mapper.toDomain(
                        entity,
                        List.of());

        assertThat(
                entity.id())
                .isEqualTo(
                        ATTEMPT_ID.value());

        assertThat(
                entity.vr008ResultId())
                .isEqualTo(
                        RESULT_ID.value());

        assertThat(
                entity.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID.value());

        assertThat(
                entity.outcome())
                .isEqualTo(
                        "SUCCEEDED");

        assertThat(
                reconstructed)
                .isEqualTo(
                        expected);
    }

    @Test
    void mapsAndReconstructsRejectedAttemptAndIssues() {
        SubmissionAttemptIssue issue =
                new SubmissionAttemptIssue(
                        new SubmissionAttemptIssueId(
                                uuid(
                                        "a4000000-0000-0000-0000-000000000006")),
                        ATTEMPT_ID,
                        SubmissionAttemptIssueType.EVENT_NOT_VALIDATED,
                        EVENT_ID,
                        null,
                        null,
                        null,
                        "Event is not validated.");

        AccountingSubmissionAttempt expected =
                AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        null,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                new SubmissionAttemptIssue(
                                        new SubmissionAttemptIssueId(
                                                uuid(
                                                        "a4000000-0000-0000-0000-000000000007")),
                                        ATTEMPT_ID,
                                        SubmissionAttemptIssueType
                                                .CONSOLIDATION_MISSING,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "No current Consolidation exists."),
                                issue));

        AccountingSubmissionAttemptEntity entity =
                mapper.toEntity(
                        expected);

        List<SubmissionAttemptIssueEntity> issueEntities =
                mapper.toIssueEntities(
                        expected);

        AccountingSubmissionAttempt reconstructed =
                mapper.toDomain(
                        entity,
                        issueEntities);

        assertThat(
                entity.outcome())
                .isEqualTo(
                        "REJECTED");

        assertThat(
                entity.consolidationId())
                .isNull();

        assertThat(
                issueEntities)
                .hasSize(
                        2);

        assertThat(
                reconstructed)
                .isEqualTo(
                        expected);
    }

    @Test
    void rejectsReconstructionOfRejectedAttemptWithoutIssues() {
        AccountingSubmissionAttempt expected =
                AccountingSubmissionAttempt.rejected(
                        ATTEMPT_ID,
                        CLOSE_ID,
                        RESULT_ID,
                        null,
                        ATTEMPTED_AT,
                        ACTOR,
                        "Submission rejected.",
                        List.of(
                                new SubmissionAttemptIssue(
                                        new SubmissionAttemptIssueId(
                                                uuid(
                                                        "a4000000-0000-0000-0000-000000000008")),
                                        ATTEMPT_ID,
                                        SubmissionAttemptIssueType
                                                .CONSOLIDATION_MISSING,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "No current Consolidation exists.")));

        AccountingSubmissionAttemptEntity entity =
                mapper.toEntity(
                        expected);

        assertThatThrownBy(
                () -> mapper.toDomain(
                        entity,
                        List.of()))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "rejected submission requires at least one issue");
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}