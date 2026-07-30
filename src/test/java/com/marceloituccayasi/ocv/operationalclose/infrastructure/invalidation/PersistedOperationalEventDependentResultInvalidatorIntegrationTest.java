package com.marceloituccayasi.ocv.operationalclose.infrastructure.invalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertState;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PersistedOperationalEventDependentResultInvalidatorIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430001");

    private static final UUID OTHER_CLOSE_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430002");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430003");

    private static final UUID OTHER_EVENT_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430004");

    private static final UUID RESULT_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430005");

    private static final UUID OTHER_RESULT_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430006");

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430007");

    private static final UUID INITIAL_TRANSITION_ID =
            UUID.fromString(
                    "e7dabd89-a5bd-4d2b-9051-b821e6430008");

    private static final OffsetDateTime EVALUATED_AT =
            OffsetDateTime.parse(
                    "2026-07-27T10:00:00Z");

    @Autowired
    private OperationalEventDependentResultInvalidator invalidator;

    @Autowired
    private EventValidationResultRepository
            validationResultRepository;

    @Autowired
    private EventValidationAlertRepository
            validationAlertRepository;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBeforeTest() {
        cleanOperationalCloseTables();
    }

    @AfterEach
    void cleanAfterTest() {
        cleanOperationalCloseTables();
    }

    @Test
    void invalidatesOnlyResultsBelongingToRequestedCloseAndPreservesAlert() {
        persistParentGraph();

        EventValidationResult revisedEventResult =
                failedResult(
                        RESULT_ID,
                        EVENT_ID);

        EventValidationResult otherCloseResult =
                failedResult(
                        OTHER_RESULT_ID,
                        OTHER_EVENT_ID);

        transactionRunner.execute(
                (Runnable) () -> {
                    validationResultRepository.saveNew(
                            revisedEventResult);

                    validationResultRepository.saveNew(
                            otherCloseResult);
                });

        EventValidationAlert activeAlert =
                EventValidationAlert.createFromFailedResult(
                        new ValidationAlertId(
                                ALERT_ID),
                        revisedEventResult,
                        "Required supporting evidence is missing.",
                        EVALUATED_AT.plusMinutes(
                                1L).toInstant(),
                        actor());

        ValidationAlertTransition initialTransition =
                ValidationAlertTransition.initial(
                        new ValidationAlertTransitionId(
                                INITIAL_TRANSITION_ID),
                        activeAlert.id(),
                        "Alert created from failed validation.",
                        activeAlert.createdAt(),
                        actor());

        validationAlertRepository.saveNew(
                activeAlert,
                initialTransition);

        invalidator.invalidateForRevisions(
                new OperationalCloseId(
                        CLOSE_ID),
                List.of(
                        new OperationalEventId(
                                EVENT_ID),
                        new OperationalEventId(
                                OTHER_EVENT_ID)));

        EventValidationResult invalidatedResult =
                validationResultRepository.findById(
                        new ValidationResultId(
                                RESULT_ID))
                        .orElseThrow();

        EventValidationResult untouchedResult =
                validationResultRepository.findById(
                        new ValidationResultId(
                                OTHER_RESULT_ID))
                        .orElseThrow();

        assertThat(invalidatedResult.current())
                .isFalse();

        assertThat(invalidatedResult.invalidatedAt())
                .isNotNull();

        assertThat(invalidatedResult.invalidationReason())
                .isEqualTo(
                        PersistedOperationalEventDependentResultInvalidator
                                .EVENT_DATA_REVISION_CHANGED);

        assertThat(
                validationResultRepository
                        .findCurrentByEventIdAndRuleCode(
                                new OperationalEventId(
                                        EVENT_ID),
                                ValidationRuleCode.VR_003))
                .isEmpty();

        assertThat(untouchedResult.current())
                .isTrue();

        assertThat(untouchedResult.invalidatedAt())
                .isNull();

        assertThat(
                validationResultRepository
                        .findCurrentByEventIdAndRuleCode(
                                new OperationalEventId(
                                        OTHER_EVENT_ID),
                                ValidationRuleCode.VR_003))
                .contains(
                        otherCloseResult);

        assertThat(
                validationAlertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                new OperationalEventId(
                                        EVENT_ID),
                                ValidationRuleCode.VR_003))
                .contains(
                        activeAlert);

        assertThat(
                validationAlertRepository.findById(
                        new ValidationAlertId(
                                ALERT_ID))
                        .orElseThrow()
                        .state())
                .isEqualTo(
                        ValidationAlertState.ACTIVE);
    }

    private static EventValidationResult failedResult(
            UUID resultId,
            UUID eventId) {

        return EventValidationResult.create(
                new ValidationResultId(
                        resultId),
                ValidationRuleCode.VR_003,
                1,
                new OperationalEventId(
                        eventId),
                ValidationOutcome.FAILED,
                "Required supporting evidence is missing.",
                EVALUATED_AT.toInstant(),
                actor(),
                3L);
    }

    private void persistParentGraph() {
        persistClose(
                CLOSE_ID,
                "2026-12-01",
                "2026-12-31");

        persistClose(
                OTHER_CLOSE_ID,
                "2027-01-01",
                "2027-01-31");

        persistEvent(
                EVENT_ID,
                CLOSE_ID,
                "Evento revisado");

        persistEvent(
                OTHER_EVENT_ID,
                OTHER_CLOSE_ID,
                "Evento de otro cierre");
    }

    private void persistClose(
            UUID closeId,
            String periodStart,
            String periodEnd) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.operational_close (
                    id,
                    period_start,
                    period_end,
                    currency_code,
                    initial_balance,
                    state,
                    state_changed_at,
                    created_at,
                    created_by_user_id,
                    created_by_username,
                    updated_at,
                    updated_by_user_id,
                    updated_by_username
                )
                VALUES (
                    ?,
                    CAST(? AS DATE),
                    CAST(? AS DATE),
                    'PEN',
                    1000.0000,
                    'PREPARATION',
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                closeId,
                periodStart,
                periodEnd,
                EVALUATED_AT,
                EVALUATED_AT,
                EVALUATED_AT);
    }

    private void persistEvent(
            UUID eventId,
            UUID closeId,
            String description) {

        jdbcTemplate.update(
                """
                INSERT INTO ocv.operational_event (
                    id,
                    close_id,
                    event_type,
                    amount,
                    balance_effect,
                    reversed_event_id,
                    occurred_at,
                    registered_at,
                    responsible_name,
                    description,
                    state,
                    evidence_required,
                    authorization_required,
                    data_revision,
                    state_changed_at,
                    created_at,
                    created_by_user_id,
                    created_by_username,
                    updated_at,
                    updated_by_user_id,
                    updated_by_username
                )
                VALUES (
                    ?,
                    ?,
                    'EXPENSE',
                    50.0000,
                    -50.0000,
                    NULL,
                    ?,
                    ?,
                    'Caja principal',
                    ?,
                    'REGISTERED',
                    TRUE,
                    FALSE,
                    3,
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                eventId,
                closeId,
                EVALUATED_AT,
                EVALUATED_AT,
                description,
                EVALUATED_AT,
                EVALUATED_AT,
                EVALUATED_AT);
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.submission_attempt_issue,
                    ocv.accounting_submission_attempt,
                    ocv.consolidation_event_snapshot,
                    ocv.consolidation,
                    ocv.alert_transition,
                    ocv.alert,
                    ocv.validation_result,
                    ocv.supporting_evidence,
                    ocv.event_authorization,
                    ocv.event_state_transition,
                    ocv.operational_event,
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

}