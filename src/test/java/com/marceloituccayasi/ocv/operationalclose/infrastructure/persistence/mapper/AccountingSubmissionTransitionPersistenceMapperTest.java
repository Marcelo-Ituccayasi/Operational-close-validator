package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.CloseStateTransitionEntity;

class AccountingSubmissionTransitionPersistenceMapperTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "cb000000-0000-0000-0000-000000000001"));

    private static final CloseStateTransitionId TRANSITION_ID =
            new CloseStateTransitionId(
                    uuid(
                            "cb000000-0000-0000-0000-000000000002"));

    private static final ValidationResultId RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "cb000000-0000-0000-0000-000000000003"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "cb000000-0000-0000-0000-000000000004"));

    private static final AccountingSubmissionAttemptId ATTEMPT_ID =
            new AccountingSubmissionAttemptId(
                    uuid(
                            "cb000000-0000-0000-0000-000000000005"));

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-30T15:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalClosePersistenceMapper mapper =
            new OperationalClosePersistenceMapper();

    @Test
    void mapsSubmissionTransitionWithAllTraceReferences() {
        CloseStateTransitionEntity entity =
                mapper.toEntity(
                        rejectedTransition(),
                        RESULT_ID,
                        CONSOLIDATION_ID,
                        ATTEMPT_ID);

        assertThat(
                entity.validationResultId())
                .isEqualTo(
                        RESULT_ID.value());

        assertThat(
                entity.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_ID.value());

        assertThat(
                entity.submissionAttemptId())
                .isEqualTo(
                        ATTEMPT_ID.value());
    }

    @Test
    void mapsSubmissionTransitionWithoutConsolidation() {
        CloseStateTransitionEntity entity =
                mapper.toEntity(
                        rejectedTransition(),
                        RESULT_ID,
                        ATTEMPT_ID);

        assertThat(
                entity.validationResultId())
                .isEqualTo(
                        RESULT_ID.value());

        assertThat(
                entity.consolidationId())
                .isNull();

        assertThat(
                entity.submissionAttemptId())
                .isEqualTo(
                        ATTEMPT_ID.value());
    }

    private CloseStateTransition rejectedTransition() {
        return new CloseStateTransition(
                TRANSITION_ID,
                CLOSE_ID,
                OperationalCloseState.VALIDATED,
                OperationalCloseState.BLOCKED,
                "ACCOUNTING_SUBMISSION_REJECTED",
                "VR-008 rejected the accounting submission.",
                OCCURRED_AT,
                ACTOR);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}