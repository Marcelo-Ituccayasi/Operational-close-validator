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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EventValidationResultPersistenceAdapterIntegrationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "99fa4a15-f12d-4db4-9905-77b7ec750001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "99fa4a15-f12d-4db4-9905-77b7ec750002");

    private static final UUID FIRST_RESULT_ID =
            UUID.fromString(
                    "99fa4a15-f12d-4db4-9905-77b7ec750003");

    private static final UUID SECOND_RESULT_ID =
            UUID.fromString(
                    "99fa4a15-f12d-4db4-9905-77b7ec750004");

    private static final UUID CLOSE_RESULT_ID =
            UUID.fromString(
                    "99fa4a15-f12d-4db4-9905-77b7ec750005");

    private static final UUID CONSOLIDATION_ID =
            UUID.fromString(
                    "99fa4a15-f12d-4db4-9905-77b7ec750006");

    private static final OffsetDateTime EVALUATED_AT =
            OffsetDateTime.parse(
                    "2026-07-26T20:00:00Z");

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
    void persistsAndReconstructsCurrentEventValidationResult() {
        persistParentCloseAndEvent();

        EventValidationResult expected =
                eventResult(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_003,
                        ValidationOutcome.FAILED,
                        "Required supporting evidence is missing.");

        transactionRunner.execute(
                (Runnable) () ->
                        validationResultRepository.saveNew(
                                expected));

        Optional<EventValidationResult> loadedById =
                validationResultRepository.findById(
                        new ValidationResultId(
                                FIRST_RESULT_ID));

        Optional<EventValidationResult> loadedCurrent =
                validationResultRepository
                        .findCurrentByEventIdAndRuleCode(
                                new OperationalEventId(
                                        EVENT_ID),
                                ValidationRuleCode.VR_003);

        Long persistedRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE id = ?
                          AND rule_code = 'VR-003'
                          AND rule_version = 1
                          AND event_id = ?
                          AND close_id IS NULL
                          AND outcome = 'FAILED'
                          AND detail =
                              'Required supporting evidence is missing.'
                          AND evaluated_by_user_id =
                              'responsible-user'
                          AND evaluated_by_username =
                              'responsible'
                          AND event_data_revision = 4
                          AND consolidation_id IS NULL
                          AND is_current = TRUE
                          AND invalidated_at IS NULL
                          AND invalidation_reason IS NULL
                        """,
                        Long.class,
                        FIRST_RESULT_ID,
                        EVENT_ID);

        assertThat(persistedRows)
                .isEqualTo(
                        1L);

        assertThat(loadedById)
                .contains(
                        expected);

        assertThat(loadedCurrent)
                .contains(
                        expected);
    }

    @Test
    void returnsCurrentEventResultsInRuleOrderAndHidesCloseResults() {
        persistParentCloseAndEvent();

        EventValidationResult vr003 =
                eventResult(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_003,
                        ValidationOutcome.FAILED,
                        "Required supporting evidence is missing.");

        EventValidationResult vr001 =
                eventResult(
                        SECOND_RESULT_ID,
                        ValidationRuleCode.VR_001,
                        ValidationOutcome.SATISFIED,
                        "Movement is registered and traceable.");

        transactionRunner.execute(
                (Runnable) () -> {
                    validationResultRepository.saveNew(
                            vr003);

                    validationResultRepository.saveNew(
                            vr001);
                });

        persistCurrentCloseResult();

        List<EventValidationResult> currentResults =
                validationResultRepository
                        .findAllCurrentByEventIdOrderByRuleCode(
                                new OperationalEventId(
                                        EVENT_ID));

        Optional<EventValidationResult> closeResult =
                validationResultRepository.findById(
                        new ValidationResultId(
                                CLOSE_RESULT_ID));

        assertThat(currentResults)
                .extracting(
                        EventValidationResult::ruleCode)
                .containsExactly(
                        ValidationRuleCode.VR_001,
                        ValidationRuleCode.VR_003);

        assertThat(currentResults)
                .extracting(
                        result -> result.id().value())
                .containsExactly(
                        SECOND_RESULT_ID,
                        FIRST_RESULT_ID);

        assertThat(closeResult)
                .isEmpty();
    }

    @Test
    void persistsOneWayInvalidationWithoutChangingEvaluationContent() {
        persistParentCloseAndEvent();

        EventValidationResult currentResult =
                eventResult(
                        FIRST_RESULT_ID,
                        ValidationRuleCode.VR_006,
                        ValidationOutcome.SATISFIED,
                        "Required authorization is present.");

        transactionRunner.execute(
                (Runnable) () ->
                        validationResultRepository.saveNew(
                                currentResult));

        EventValidationResult invalidatedResult =
                currentResult.invalidate(
                        EVALUATED_AT.plusSeconds(
                                60L).toInstant(),
                        "Event data revision changed.");

        transactionRunner.execute(
                (Runnable) () ->
                        validationResultRepository.saveInvalidation(
                                invalidatedResult));

        Optional<EventValidationResult> loadedById =
                validationResultRepository.findById(
                        new ValidationResultId(
                                FIRST_RESULT_ID));

        Optional<EventValidationResult> loadedCurrent =
                validationResultRepository
                        .findCurrentByEventIdAndRuleCode(
                                new OperationalEventId(
                                        EVENT_ID),
                                ValidationRuleCode.VR_006);

        String persistedMetadata =
                jdbcTemplate.queryForObject(
                        """
                        SELECT
                            outcome
                            || '|'
                            || detail
                            || '|'
                            || is_current
                            || '|'
                            || invalidation_reason
                        FROM ocv.validation_result
                        WHERE id = ?
                        """,
                        String.class,
                        FIRST_RESULT_ID);

        assertThat(loadedById)
                .contains(
                        invalidatedResult);

        assertThat(loadedCurrent)
                .isEmpty();

        assertThat(persistedMetadata)
                .isEqualTo(
                        "SATISFIED|Required authorization is present."
                                + "|false|Event data revision changed.");
    }

    private EventValidationResult eventResult(
            UUID resultId,
            ValidationRuleCode ruleCode,
            ValidationOutcome outcome,
            String detail) {

        return EventValidationResult.create(
                new ValidationResultId(
                        resultId),
                ruleCode,
                1,
                new OperationalEventId(
                        EVENT_ID),
                outcome,
                detail,
                EVALUATED_AT.toInstant(),
                actor(),
                4L);
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
                    DATE '2026-11-01',
                    DATE '2026-11-30',
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
                    'Evento para persistencia de resultados de validación',
                    'REGISTERED',
                    TRUE,
                    TRUE,
                    4,
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

    private void persistCurrentCloseResult() {
        jdbcTemplate.update(
                """
                INSERT INTO ocv.consolidation (
                    id,
                    close_id,
                    currency_code,
                    event_count,
                    total_income,
                    total_expense,
                    total_discount,
                    total_cancellation,
                    initial_balance,
                    expected_balance,
                    actual_balance,
                    difference,
                    is_current,
                    completed_at,
                    completed_by_user_id,
                    completed_by_username,
                    invalidated_at,
                    invalidation_reason
                )
                VALUES (
                    ?,
                    ?,
                    'PEN',
                    1,
                    0.0000,
                    50.0000,
                    0.0000,
                    0.0000,
                    1000.0000,
                    950.0000,
                    950.0000,
                    0.0000,
                    TRUE,
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL,
                    NULL
                )
                """,
                CONSOLIDATION_ID,
                CLOSE_ID,
                EVALUATED_AT);

        jdbcTemplate.update(
                """
                INSERT INTO ocv.validation_result (
                    id,
                    rule_code,
                    rule_version,
                    event_id,
                    close_id,
                    outcome,
                    detail,
                    evaluated_at,
                    evaluated_by_user_id,
                    evaluated_by_username,
                    event_data_revision,
                    consolidation_id,
                    is_current,
                    invalidated_at,
                    invalidation_reason
                )
                VALUES (
                    ?,
                    'VR-008',
                    1,
                    NULL,
                    ?,
                    'SATISFIED',
                    'Known-event consolidation is complete.',
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL,
                    ?,
                    TRUE,
                    NULL,
                    NULL
                )
                """,
                CLOSE_RESULT_ID,
                CLOSE_ID,
                EVALUATED_AT,
                CONSOLIDATION_ID);
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