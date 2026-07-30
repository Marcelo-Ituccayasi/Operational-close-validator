package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlertChange;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
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
class EventValidationAlertPersistenceAdapterIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0002");

    private static final UUID FAILED_RESULT_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0003");

    private static final UUID SATISFIED_RESULT_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0004");

    private static final UUID ALERT_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0005");

    private static final UUID INITIAL_TRANSITION_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0006");

    private static final UUID ACKNOWLEDGED_TRANSITION_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0007");

    private static final UUID RESOLUTION_TRANSITION_ID =
            UUID.fromString(
                    "cb66b46a-4fd4-4201-a3f7-e61f665a0008");

    private static final OffsetDateTime EVALUATED_AT =
            OffsetDateTime.parse(
                    "2026-07-27T02:00:00Z");

    @Autowired
    private EventValidationAlertRepository
            validationAlertRepository;

    @Autowired
    private EventValidationResultRepository
            validationResultRepository;

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
    void persistsAndReconstructsAlertWithInitialHistory() {
        persistParentCloseAndEvent();

        EventValidationResult failedResult =
                persistFailedResult();

        EventValidationAlert alert =
                activeAlert(
                        failedResult);

        ValidationAlertTransition initialTransition =
                initialTransition();

        validationAlertRepository.saveNew(
                alert,
                initialTransition);

        Optional<EventValidationAlert> loaded =
                validationAlertRepository.findById(
                        new ValidationAlertId(
                                ALERT_ID));

        Optional<EventValidationAlert> openAlert =
                validationAlertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                new OperationalEventId(
                                        EVENT_ID),
                                ValidationRuleCode.VR_003);

        List<ValidationAlertTransition> history =
                validationAlertRepository
                        .findHistoryByAlertIdOrderByOccurredAt(
                                new ValidationAlertId(
                                        ALERT_ID));

        Long persistedAlerts =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert
                        WHERE id = ?
                          AND event_id = ?
                          AND close_id IS NULL
                          AND source_validation_result_id = ?
                          AND cause_code = 'VR-003'
                          AND severity = 'HIGH'
                          AND is_blocking = TRUE
                          AND state = 'ACTIVE'
                          AND detail =
                              'Required supporting evidence is missing.'
                          AND resolved_by_validation_result_id IS NULL
                          AND discard_justification IS NULL
                          AND created_by_user_id =
                              'responsible-user'
                          AND created_by_username =
                              'responsible'
                          AND closed_at IS NULL
                        """,
                        Long.class,
                        ALERT_ID,
                        EVENT_ID,
                        FAILED_RESULT_ID);

        Long persistedTransitions =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert_transition
                        WHERE id = ?
                          AND alert_id = ?
                          AND from_state IS NULL
                          AND to_state = 'ACTIVE'
                          AND action_code = 'ALERT_CREATED'
                          AND validation_result_id IS NULL
                          AND actor_user_id =
                              'responsible-user'
                          AND actor_username =
                              'responsible'
                        """,
                        Long.class,
                        INITIAL_TRANSITION_ID,
                        ALERT_ID);

        assertThat(persistedAlerts)
                .isEqualTo(
                        1L);

        assertThat(persistedTransitions)
                .isEqualTo(
                        1L);

        assertThat(loaded)
                .contains(
                        alert);

        assertThat(openAlert)
                .contains(
                        alert);

        assertThat(history)
                .containsExactly(
                        initialTransition);
    }

    @Test
    void persistsAcknowledgementAndLocksExistingAlert() {
        persistParentCloseAndEvent();

        EventValidationResult failedResult =
                persistFailedResult();

        EventValidationAlert alert =
                activeAlert(
                        failedResult);

        validationAlertRepository.saveNew(
                alert,
                initialTransition());

        EventValidationAlertChange acknowledged =
                alert.acknowledge(
                        new ValidationAlertTransitionId(
                                ACKNOWLEDGED_TRANSITION_ID),
                        "Responsible user acknowledged the alert.",
                        EVALUATED_AT.plusMinutes(
                                2L).toInstant(),
                        actor());

        validationAlertRepository.saveChange(
                acknowledged);

        EventValidationAlert lockedAlert =
                transactionRunner.execute(
                        () -> validationAlertRepository
                                .findByIdForUpdate(
                                        new ValidationAlertId(
                                                ALERT_ID))
                                .orElseThrow());

        List<EventValidationAlert> openAlerts =
                validationAlertRepository
                        .findAllOpenByEventIdOrderByCreatedAt(
                                new OperationalEventId(
                                        EVENT_ID));

        List<ValidationAlertTransition> history =
                validationAlertRepository
                        .findHistoryByAlertIdOrderByOccurredAt(
                                new ValidationAlertId(
                                        ALERT_ID));

        assertThat(lockedAlert)
                .isEqualTo(
                        acknowledged.alert());

        assertThat(lockedAlert.state())
                .isEqualTo(
                        ValidationAlertState.ACKNOWLEDGED);

        assertThat(openAlerts)
                .containsExactly(
                        acknowledged.alert());

        assertThat(history)
                .containsExactly(
                        initialTransition(),
                        acknowledged.transition());
    }

    @Test
    void resolvesAlertWithCurrentSatisfiedRevalidation() {
        persistParentCloseAndEvent();

        EventValidationResult failedResult =
                persistFailedResult();

        EventValidationAlert alert =
                activeAlert(
                        failedResult);

        validationAlertRepository.saveNew(
                alert,
                initialTransition());

        EventValidationResult invalidatedFailedResult =
                failedResult.invalidate(
                        EVALUATED_AT.plusMinutes(
                                2L).toInstant(),
                        "Superseded by satisfactory revalidation.");

        transactionRunner.execute(
                (Runnable) () ->
                        validationResultRepository.saveInvalidation(
                                invalidatedFailedResult));

        EventValidationResult satisfiedResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                SATISFIED_RESULT_ID),
                        ValidationRuleCode.VR_003,
                        1,
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationOutcome.SATISFIED,
                        "Required supporting evidence is present.",
                        EVALUATED_AT.plusMinutes(
                                3L).toInstant(),
                        actor(),
                        3L);

        transactionRunner.execute(
                (Runnable) () ->
                        validationResultRepository.saveNew(
                                satisfiedResult));

        EventValidationAlertChange resolved =
                alert.resolve(
                        new ValidationAlertTransitionId(
                                RESOLUTION_TRANSITION_ID),
                        satisfiedResult,
                        "Resolved after satisfactory revalidation.",
                        EVALUATED_AT.plusMinutes(
                                4L).toInstant(),
                        actor());

        validationAlertRepository.saveChange(
                resolved);

        Optional<EventValidationAlert> loaded =
                validationAlertRepository.findById(
                        new ValidationAlertId(
                                ALERT_ID));

        Optional<EventValidationAlert> openAlert =
                validationAlertRepository
                        .findOpenByEventIdAndCauseRuleCode(
                                new OperationalEventId(
                                        EVENT_ID),
                                ValidationRuleCode.VR_003);

        List<ValidationAlertTransition> history =
                validationAlertRepository
                        .findHistoryByAlertIdOrderByOccurredAt(
                                new ValidationAlertId(
                                        ALERT_ID));

        String persistedResolution =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            state
                            || '|'
                            || resolved_by_validation_result_id
                            || '|'
                            || is_blocking
                        FROM ocv.alert
                        WHERE id = ?
                        """,
                        String.class,
                        ALERT_ID);

        assertThat(loaded)
                .contains(
                        resolved.alert());

        assertThat(openAlert)
                .isEmpty();

        assertThat(
                validationAlertRepository
                        .findAllOpenByEventIdOrderByCreatedAt(
                                new OperationalEventId(
                                        EVENT_ID)))
                .isEmpty();

        assertThat(history)
                .containsExactly(
                        initialTransition(),
                        resolved.transition());

        assertThat(persistedResolution)
                .isEqualTo(
                        "RESOLVED|"
                                + SATISFIED_RESULT_ID
                                + "|true");
    }

    private EventValidationResult persistFailedResult() {
        EventValidationResult failedResult =
                EventValidationResult.create(
                        new ValidationResultId(
                                FAILED_RESULT_ID),
                        ValidationRuleCode.VR_003,
                        1,
                        new OperationalEventId(
                                EVENT_ID),
                        ValidationOutcome.FAILED,
                        "Required supporting evidence is missing.",
                        EVALUATED_AT.toInstant(),
                        actor(),
                        3L);

        transactionRunner.execute(
                (Runnable) () ->
                        validationResultRepository.saveNew(
                                failedResult));

        return failedResult;
    }

    private static EventValidationAlert activeAlert(
            EventValidationResult failedResult) {

        return EventValidationAlert.createFromFailedResult(
                new ValidationAlertId(
                        ALERT_ID),
                failedResult,
                "Required supporting evidence is missing.",
                EVALUATED_AT.plusMinutes(
                        1L).toInstant(),
                actor());
    }

    private static ValidationAlertTransition initialTransition() {
        return ValidationAlertTransition.initial(
                new ValidationAlertTransitionId(
                        INITIAL_TRANSITION_ID),
                new ValidationAlertId(
                        ALERT_ID),
                "Alert created from failed validation.",
                EVALUATED_AT.plusMinutes(
                        1L).toInstant(),
                actor());
    }

    private void persistParentCloseAndEvent() {
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
                    DATE '2026-12-01',
                    DATE '2026-12-31',
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
                CLOSE_ID,
                EVALUATED_AT,
                EVALUATED_AT,
                EVALUATED_AT);

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
                    'Evento para persistencia de alertas de validación',
                    'REGISTERED',
                    TRUE,
                    TRUE,
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
                EVENT_ID,
                CLOSE_ID,
                EVALUATED_AT,
                EVALUATED_AT,
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