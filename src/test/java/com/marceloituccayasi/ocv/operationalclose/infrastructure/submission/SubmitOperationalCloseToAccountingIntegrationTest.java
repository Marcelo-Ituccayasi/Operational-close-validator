package com.marceloituccayasi.ocv.operationalclose.infrastructure.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.operationalclose.application.CloseConsolidationReadinessEvaluator;
import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccounting;
import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccountingCommand;
import com.marceloituccayasi.ocv.operationalclose.application.SubmitOperationalCloseToAccountingResult;
import com.marceloituccayasi.ocv.operationalclose.application.Vr008Evaluator;
import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.BlockingCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.FailureController;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.FailurePoint;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.FaultInjectingAttemptRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.FaultInjectingCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.FaultInjectingCloseValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.FaultInjectingConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.InjectedSubmissionFailure;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.submission.AccountingSubmissionIntegrationTestSupport.LockProbe;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.AccountingSubmissionAttemptPersistenceAdapter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.CloseValidationResultPersistenceAdapter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.ConsolidationPersistenceAdapter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.EventValidationAlertPersistenceAdapter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.EventValidationResultPersistenceAdapter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.OperationalClosePersistenceAdapter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter.OperationalEventPersistenceAdapter;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties =
                "ocv.persistence.pessimistic-lock-timeout-ms=5000")
class SubmitOperationalCloseToAccountingIntegrationTest {

    private static final UUID CLOSE_ID =
            uuid(
                    "ce000000-0000-0000-0000-000000000001");

    private static final UUID EVENT_ID =
            uuid(
                    "ce000000-0000-0000-0000-000000000002");

    private static final UUID CONSOLIDATION_ID =
            uuid(
                    "ce000000-0000-0000-0000-000000000003");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-30T16:00:00Z");

    @Autowired
    private OperationalClosePersistenceAdapter
            closePersistenceAdapter;

    @Autowired
    private OperationalEventPersistenceAdapter
            eventPersistenceAdapter;

    @Autowired
    private EventValidationResultPersistenceAdapter
            eventValidationResultPersistenceAdapter;

    @Autowired
    private EventValidationAlertPersistenceAdapter
            eventValidationAlertPersistenceAdapter;

    @Autowired
    private ConsolidationPersistenceAdapter
            consolidationPersistenceAdapter;

    @Autowired
    private CloseValidationResultPersistenceAdapter
            closeValidationResultPersistenceAdapter;

    @Autowired
    private AccountingSubmissionAttemptPersistenceAdapter
            submissionAttemptPersistenceAdapter;

    @Autowired
    private CloseConsolidationReadinessEvaluator
            readinessEvaluator;

    @Autowired
    private Vr008Evaluator vr008Evaluator;

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FailureController failureController;

    private LockProbe lockProbe;

    private SubmitOperationalCloseToAccounting useCase;

    @BeforeEach
    void prepareTest() {
        cleanOperationalCloseTables();

        failureController =
                new FailureController();

        lockProbe =
                new LockProbe();

        useCase =
                new SubmitOperationalCloseToAccounting(
                        new BlockingCloseLockRepository(
                                closePersistenceAdapter,
                                lockProbe),
                        new FaultInjectingCloseRevisionRepository(
                                closePersistenceAdapter,
                                failureController),
                        eventPersistenceAdapter,
                        eventValidationResultPersistenceAdapter,
                        eventValidationAlertPersistenceAdapter,
                        new FaultInjectingConsolidationRepository(
                                consolidationPersistenceAdapter,
                                failureController),
                        new FaultInjectingCloseValidationResultRepository(
                                closeValidationResultPersistenceAdapter,
                                failureController),
                        new FaultInjectingAttemptRepository(
                                submissionAttemptPersistenceAdapter,
                                failureController),
                        readinessEvaluator,
                        vr008Evaluator,
                        () ->
                                new AuthenticatedPrincipal(
                                        AuditActor.RESPONSIBLE_USER_ID,
                                        "responsible"),
                        () -> NOW,
                        uuidGenerator,
                        transactionRunner);
    }

    @AfterEach
    void cleanAfterTest() {
        lockProbe.releaseFirstLock();
        cleanOperationalCloseTables();
    }

    @Test
    void submitsCompleteCloseAtomically() {
        persistSuccessfulFixture();

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.SUBMITTED);

        assertThat(
                closeState())
                .isEqualTo(
                        "SENT_TO_ACCOUNTING");

        assertThat(
                textValue(
                        """
                        SELECT outcome
                        FROM ocv.validation_result
                        WHERE id = ?
                        """,
                        result.validationResultId()))
                .isEqualTo(
                        "SATISFIED");

        assertThat(
                uuidValue(
                        """
                        SELECT consolidation_id
                        FROM ocv.validation_result
                        WHERE id = ?
                        """,
                        result.validationResultId()))
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                textValue(
                        """
                        SELECT outcome
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        "SUCCEEDED");

        assertThat(
                uuidValue(
                        """
                        SELECT vr008_result_id
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        result.validationResultId());

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        WHERE submission_attempt_id = ?
                        """,
                        result.submissionAttemptId()))
                .isZero();

        assertThat(
                textValue(
                        """
                        SELECT cause_code
                        FROM ocv.close_state_transition
                        WHERE submission_attempt_id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        SubmitOperationalCloseToAccounting
                                .ACCOUNTING_SUBMISSION_SUCCEEDED);

        assertThat(
                uuidValue(
                        """
                        SELECT validation_result_id
                        FROM ocv.close_state_transition
                        WHERE submission_attempt_id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        result.validationResultId());

        assertThat(
                uuidValue(
                        """
                        SELECT consolidation_id
                        FROM ocv.close_state_transition
                        WHERE submission_attempt_id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                currentConsolidationCount())
                .isEqualTo(
                        1L);
    }

    @Test
    void persistsRejectionAndBlocksCloseWhenConsolidationIsMissing() {
        persistMissingConsolidationFixture();

        SubmitOperationalCloseToAccountingResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        SubmitOperationalCloseToAccountingResult
                                .Status.SUBMISSION_REJECTED);

        assertThat(
                result.issueTypes())
                .containsExactly(
                        SubmissionAttemptIssueType
                                .CONSOLIDATION_MISSING);

        assertThat(
                closeState())
                .isEqualTo(
                        "BLOCKED");

        assertThat(
                textValue(
                        """
                        SELECT outcome
                        FROM ocv.validation_result
                        WHERE id = ?
                        """,
                        result.validationResultId()))
                .isEqualTo(
                        "FAILED");

        assertThat(
                textValue(
                        """
                        SELECT outcome
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        "REJECTED");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                          AND consolidation_id IS NULL
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        1L);

        assertThat(
                textValue(
                        """
                        SELECT issue_type
                        FROM ocv.submission_attempt_issue
                        WHERE submission_attempt_id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        "CONSOLIDATION_MISSING");

        assertThat(
                textValue(
                        """
                        SELECT cause_code
                        FROM ocv.close_state_transition
                        WHERE submission_attempt_id = ?
                        """,
                        result.submissionAttemptId()))
                .isEqualTo(
                        SubmitOperationalCloseToAccounting
                                .ACCOUNTING_SUBMISSION_REJECTED);

        assertThat(
                currentConsolidationCount())
                .isZero();
    }

    @Test
    void rollsBackAfterValidationResultPersistence() {
        assertSuccessfulPathRollback(
                FailurePoint.AFTER_VALIDATION_RESULT_SAVE);
    }

    @Test
    void rollsBackAfterAttemptPersistence() {
        assertSuccessfulPathRollback(
                FailurePoint.AFTER_ATTEMPT_SAVE);
    }

    @Test
    void rollsBackAfterCloseRevisionPersistence() {
        assertSuccessfulPathRollback(
                FailurePoint.AFTER_CLOSE_REVISION_SAVE);
    }

    @Test
    void rollsBackAfterTransitionPersistence() {
        assertSuccessfulPathRollback(
                FailurePoint.AFTER_TRANSITION_SAVE);
    }

    @Test
    void rollsBackRejectedConsolidationInvalidation() {
        persistStaleConsolidationFixture();

        failureController.failAt(
                FailurePoint
                        .AFTER_CONSOLIDATION_INVALIDATION);

        assertThatThrownBy(
                () -> useCase.execute(
                        command()))
                .isInstanceOf(
                        InjectedSubmissionFailure.class)
                .hasMessageContaining(
                        FailurePoint
                                .AFTER_CONSOLIDATION_INVALIDATION
                                .name());

        assertUnsubmittedFixtureRemains();

        assertThat(
                currentConsolidationCount())
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE id = ?
                          AND invalidated_at IS NULL
                          AND invalidation_reason IS NULL
                        """,
                        CONSOLIDATION_ID))
                .isEqualTo(
                        1L);
    }

    @Test
    void serializesTwoRealTransactionsAndCreatesOneSubmission() throws Exception {
        persistSuccessfulFixture();

        lockProbe.holdFirstLock();

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        2);

        Future<SubmitOperationalCloseToAccountingResult>
                firstFuture =
                        null;

        Future<SubmitOperationalCloseToAccountingResult>
                secondFuture =
                        null;

        try {
            firstFuture =
                    executor.submit(
                            () -> useCase.execute(
                                    command()));

            assertThat(
                    lockProbe.awaitFirstLock())
                    .isTrue();

            secondFuture =
                    executor.submit(
                            () -> useCase.execute(
                                    command()));

            assertThat(
                    lockProbe.awaitSecondAttempt())
                    .isTrue();

            assertThat(
                    secondFuture.isDone())
                    .isFalse();

            lockProbe.releaseFirstLock();

            SubmitOperationalCloseToAccountingResult first =
                    firstFuture.get(
                            10,
                            TimeUnit.SECONDS);

            SubmitOperationalCloseToAccountingResult second =
                    secondFuture.get(
                            10,
                            TimeUnit.SECONDS);

            assertThat(
                    first.status())
                    .isEqualTo(
                            SubmitOperationalCloseToAccountingResult
                                    .Status.SUBMITTED);

            assertThat(
                    second.status())
                    .isEqualTo(
                            SubmitOperationalCloseToAccountingResult
                                    .Status.CLOSE_ALREADY_SUBMITTED);
        }
        finally {
            lockProbe.releaseFirstLock();

            if (firstFuture != null
                    && !firstFuture.isDone()) {

                firstFuture.cancel(
                        true);
            }

            if (secondFuture != null
                    && !secondFuture.isDone()) {

                secondFuture.cancel(
                        true);
            }

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS);
        }

        assertThat(
                lockProbe.startedAttempts())
                .isEqualTo(
                        2);

        assertThat(
                closeState())
                .isEqualTo(
                        "SENT_TO_ACCOUNTING");

        assertThat(
                vr008ResultCount())
                .isEqualTo(
                        1L);

        assertThat(
                attemptCount())
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.accounting_submission_attempt
                        WHERE close_id = ?
                          AND outcome = 'SUCCEEDED'
                        """,
                        CLOSE_ID))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND cause_code = ?
                        """,
                        CLOSE_ID,
                        SubmitOperationalCloseToAccounting
                                .ACCOUNTING_SUBMISSION_SUCCEEDED))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        """))
                .isZero();
    }

    private void assertSuccessfulPathRollback(
            FailurePoint failurePoint) {

        persistSuccessfulFixture();

        failureController.failAt(
                failurePoint);

        assertThatThrownBy(
                () -> useCase.execute(
                        command()))
                .isInstanceOf(
                        InjectedSubmissionFailure.class)
                .hasMessageContaining(
                        failurePoint.name());

        assertUnsubmittedFixtureRemains();

        assertThat(
                currentConsolidationCount())
                .isEqualTo(
                        1L);
    }

    private void assertUnsubmittedFixtureRemains() {
        assertThat(
                closeState())
                .isEqualTo(
                        "VALIDATED");

        assertThat(
                vr008ResultCount())
                .isZero();

        assertThat(
                attemptCount())
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        """))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND submission_attempt_id IS NOT NULL
                        """,
                        CLOSE_ID))
                .isZero();
    }

    private void persistSuccessfulFixture() {
        persistClose();
        persistEvent(
                1L);
        persistConsolidation(
                1L);
    }

    private void persistMissingConsolidationFixture() {
        persistClose();
        persistEvent(
                1L);
    }

    private void persistStaleConsolidationFixture() {
        persistClose();
        persistEvent(
                2L);
        persistConsolidation(
                1L);
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
                CLOSE_ID,
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT));
    }

    private void persistEvent(
            long dataRevision) {

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
                    100.0000,
                    -100.0000,
                    NULL,
                    ?,
                    ?,
                    'Caja principal',
                    'Egreso para prueba de envÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­o contable',
                    'VALIDATED',
                    FALSE,
                    FALSE,
                    ?,
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
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                dataRevision,
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT));
    }

    private void persistConsolidation(
            long snapshotRevision) {

        Instant completedAt =
                NOW.minusSeconds(
                        60);

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
                    100.0000,
                    0.0000,
                    0.0000,
                    1000.0000,
                    900.0000,
                    900.0000,
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
                databaseTimestamp(
                        completedAt));

        jdbcTemplate.update(
                """
                INSERT INTO ocv.consolidation_event_snapshot (
                    consolidation_id,
                    event_id,
                    event_data_revision,
                    event_type,
                    amount,
                    balance_effect,
                    reversed_event_id,
                    event_state,
                    captured_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    'EXPENSE',
                    100.0000,
                    -100.0000,
                    NULL,
                    'VALIDATED',
                    ?
                )
                """,
                CONSOLIDATION_ID,
                EVENT_ID,
                snapshotRevision,
                databaseTimestamp(
                        completedAt));
    }

    private SubmitOperationalCloseToAccountingCommand command() {
        return new SubmitOperationalCloseToAccountingCommand(
                CLOSE_ID);
    }

    private String closeState() {
        return textValue(
                """
                SELECT state
                FROM ocv.operational_close
                WHERE id = ?
                """,
                CLOSE_ID);
    }

    private long currentConsolidationCount() {
        return count(
                """
                SELECT COUNT(*)
                FROM ocv.consolidation
                WHERE close_id = ?
                  AND is_current = TRUE
                """,
                CLOSE_ID);
    }

    private long vr008ResultCount() {
        return count(
                """
                SELECT COUNT(*)
                FROM ocv.validation_result
                WHERE close_id = ?
                  AND rule_code = 'VR-008'
                """,
                CLOSE_ID);
    }

    private long attemptCount() {
        return count(
                """
                SELECT COUNT(*)
                FROM ocv.accounting_submission_attempt
                WHERE close_id = ?
                """,
                CLOSE_ID);
    }

    private String textValue(
            String sql,
            Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                String.class,
                arguments);
    }

    private UUID uuidValue(
            String sql,
            Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                arguments);
    }

    private long count(
            String sql,
            Object... arguments) {

        Long result =
                jdbcTemplate.queryForObject(
                        sql,
                        Long.class,
                        arguments);

        return result == null
                ? 0L
                : result;
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

    private static OffsetDateTime databaseTimestamp(
            Instant instant) {

        return OffsetDateTime.ofInstant(
                instant,
                ZoneOffset.UTC);
    }


    private static UUID uuid(
            String value) {

        return UUID.fromString(
                value);
    }

}