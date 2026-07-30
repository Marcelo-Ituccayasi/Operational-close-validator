package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.CloseValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CloseValidationResultPersistenceAdapterIntegrationTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    uuid(
                            "b8000000-0000-0000-0000-000000000001"));

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    uuid(
                            "b8000000-0000-0000-0000-000000000002"));

    private static final ValidationResultId FIRST_RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "b8000000-0000-0000-0000-000000000003"));

    private static final ValidationResultId SECOND_RESULT_ID =
            new ValidationResultId(
                    uuid(
                            "b8000000-0000-0000-0000-000000000004"));

    private static final Instant FIRST_EVALUATED_AT =
            Instant.parse(
                    "2026-07-30T20:00:00Z");

    private static final Instant SECOND_EVALUATED_AT =
            Instant.parse(
                    "2026-07-30T21:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    @Autowired
    private CloseValidationResultRepository resultRepository;

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
    void persistsAndLoadsCurrentSatisfiedVr008Result() {
        persistClose();
        persistConsolidation();

        CloseValidationResult expected =
                satisfiedResult(
                        FIRST_RESULT_ID,
                        FIRST_EVALUATED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveNew(
                                expected));

        Optional<CloseValidationResult> loadedById =
                resultRepository.findById(
                        FIRST_RESULT_ID);

        Optional<CloseValidationResult> loadedCurrent =
                resultRepository
                        .findCurrentByCloseIdAndRuleCode(
                                CLOSE_ID,
                                ValidationRuleCode.VR_008);

        assertThat(
                loadedById)
                .contains(
                        expected);

        assertThat(
                loadedCurrent)
                .contains(
                        expected);
    }

    @Test
    void persistsFailedVr008WithoutConsolidation() {
        persistClose();

        CloseValidationResult expected =
                failedResult(
                        FIRST_RESULT_ID,
                        FIRST_EVALUATED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveNew(
                                expected));

        assertThat(
                resultRepository.findById(
                        FIRST_RESULT_ID))
                .contains(
                        expected);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE id = ?
                          AND event_id IS NULL
                          AND close_id = ?
                          AND consolidation_id IS NULL
                        """,
                        FIRST_RESULT_ID.value(),
                        CLOSE_ID.value()))
                .isEqualTo(
                        1);
    }

    @Test
    void persistsInvalidationAndPermitsReplacement() {
        persistClose();

        CloseValidationResult first =
                failedResult(
                        FIRST_RESULT_ID,
                        FIRST_EVALUATED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveNew(
                                first));

        CloseValidationResult invalidated =
                first.invalidate(
                        SECOND_EVALUATED_AT,
                        "Superseded by another VR-008 evaluation.");

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveInvalidation(
                                invalidated));

        CloseValidationResult second =
                failedResult(
                        SECOND_RESULT_ID,
                        SECOND_EVALUATED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveNew(
                                second));

        assertThat(
                resultRepository.findById(
                        FIRST_RESULT_ID))
                .contains(
                        invalidated);

        assertThat(
                resultRepository
                        .findCurrentByCloseIdAndRuleCode(
                                CLOSE_ID,
                                ValidationRuleCode.VR_008))
                .contains(
                        second);
    }

    @Test
    void rejectsSecondCurrentVr008Result() {
        persistClose();

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveNew(
                                failedResult(
                                        FIRST_RESULT_ID,
                                        FIRST_EVALUATED_AT)));

        assertThatThrownBy(
                () -> transactionRunner.execute(
                        (Runnable) () ->
                                resultRepository.saveNew(
                                        failedResult(
                                                SECOND_RESULT_ID,
                                                SECOND_EVALUATED_AT))))
                .isInstanceOf(
                        DataIntegrityViolationException.class);
    }

    @Test
    void eventRepositoryCannotLoadCloseResultById() {
        persistClose();

        CloseValidationResult result =
                failedResult(
                        FIRST_RESULT_ID,
                        FIRST_EVALUATED_AT);

        transactionRunner.execute(
                (Runnable) () ->
                        resultRepository.saveNew(
                                result));

        Integer eventScopedCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE id = ?
                          AND event_id IS NOT NULL
                        """,
                        Integer.class,
                        FIRST_RESULT_ID.value());

        assertThat(
                eventScopedCount)
                .isZero();
    }

    private CloseValidationResult satisfiedResult(
            ValidationResultId resultId,
            Instant evaluatedAt) {

        return CloseValidationResult.create(
                resultId,
                ValidationRuleCode.VR_008,
                1,
                CLOSE_ID,
                ValidationOutcome.SATISFIED,
                "Final control passed.",
                evaluatedAt,
                ACTOR,
                CONSOLIDATION_ID);
    }

    private CloseValidationResult failedResult(
            ValidationResultId resultId,
            Instant evaluatedAt) {

        return CloseValidationResult.create(
                resultId,
                ValidationRuleCode.VR_008,
                1,
                CLOSE_ID,
                ValidationOutcome.FAILED,
                "Final control failed.",
                evaluatedAt,
                ACTOR,
                null);
    }

    private void persistClose() {
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
                    DATE '2026-08-01',
                    DATE '2026-08-31',
                    'PEN',
                    1000.0000,
                    'VALIDATED',
                    ?,
                    ?,
                    'responsible-user',
                    'responsible',
                    ?,
                    'responsible-user',
                    'responsible'
                )
                """,
                CLOSE_ID.value(),
                databaseTimestamp(
                        FIRST_EVALUATED_AT),
                databaseTimestamp(
                        FIRST_EVALUATED_AT),
                databaseTimestamp(
                        FIRST_EVALUATED_AT));
    }

    private void persistConsolidation() {
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
                    100.0000,
                    0.0000,
                    0.0000,
                    0.0000,
                    1000.0000,
                    1100.0000,
                    1100.0000,
                    0.0000,
                    TRUE,
                    ?,
                    'responsible-user',
                    'responsible',
                    NULL,
                    NULL
                )
                """,
                CONSOLIDATION_ID.value(),
                CLOSE_ID.value(),
                databaseTimestamp(
                        FIRST_EVALUATED_AT.minusSeconds(
                                60)));
    }

    private long count(
            String sql,
            Object... arguments) {

        Long value =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class,
                        arguments);

        return value == null
                ? 0L
                : value;
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

    private static java.time.OffsetDateTime databaseTimestamp(
            Instant instant) {

        return java.time.OffsetDateTime.ofInstant(
                instant,
                java.time.ZoneOffset.UTC);
    }

    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}