package com.marceloituccayasi.ocv.integration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.provisioning.ResponsibleUserProvisioner;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OperationalCloseAccountingSubmissionWebIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

    private static final String SUBMITTED_MESSAGE =
            "Cierre enviado a contabilidad.";

    private static final String REJECTED_MESSAGE =
            "El envío fue rechazado. Revisa las causas registradas.";

    private static final UUID CLOSE_ID =
            uuid(
                    "cf000000-0000-0000-0000-000000000001");

    private static final UUID EVENT_ID =
            uuid(
                    "cf000000-0000-0000-0000-000000000002");

    private static final UUID CONSOLIDATION_ID =
            uuid(
                    "cf000000-0000-0000-0000-000000000003");

    private static final UUID MISSING_CLOSE_ID =
            uuid(
                    "cf000000-0000-0000-0000-000000000099");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-01-01T12:00:00Z");

    private static final Instant CONSOLIDATED_AT =
            Instant.parse(
                    "2026-01-02T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityUserJpaRepository identityRepository;

    @Autowired
    private ResponsibleUserProvisioner provisioner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareTestState() {
        clearRegisteredSessions();
        cleanOperationalCloseTables();

        provisioner.provision();

        IdentityUserEntity identityUser =
                identityRepository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        identityUser.synchronize(
                "responsible",
                "responsible",
                passwordEncoder.encode(
                        TEST_PASSWORD),
                Instant.now());

        identityRepository.saveAndFlush(
                identityUser);
    }

    @AfterEach
    void cleanTestState() {
        cleanOperationalCloseTables();
        clearRegisteredSessions();
    }

    @Test
    void protectsSubmissionRouteAndRequiresCsrf()
            throws Exception {

        String submissionUrl =
                submissionUrl(
                        CLOSE_ID);

        mockMvc.perform(
                        post(
                                submissionUrl)
                                .with(
                                        csrf()))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login"));

        MockHttpSession session =
                authenticatedSession();

        persistSuccessfulFixture();

        mockMvc.perform(
                        post(
                                submissionUrl)
                                .session(
                                        session))
                .andExpect(
                        status().isForbidden());

        assertThat(
                closeState())
                .isEqualTo(
                        "VALIDATED");

        assertThat(
                attemptCount())
                .isZero();
    }

    @Test
    void submitsValidatedCloseAndRedirectsWithSuccessMessage()
            throws Exception {

        persistSuccessfulFixture();

        MockHttpSession session =
                authenticatedSession();

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + CLOSE_ID)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        submissionUrl(
                                                CLOSE_ID))))
                .andExpect(
                        content().string(
                                containsString(
                                        "Enviar a contabilidad")))
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"_csrf\"")));

        MvcResult submissionResult =
                mockMvc.perform(
                                post(
                                        submissionUrl(
                                                CLOSE_ID))
                                        .session(
                                                session)
                                        .with(
                                                csrf())
                                        .param(
                                                "state",
                                                "BLOCKED")
                                        .param(
                                                "outcome",
                                                "REJECTED")
                                        .param(
                                                "actor",
                                                "another-user")
                                        .param(
                                                "attemptedAt",
                                                "2000-01-01T00:00:00Z")
                                        .param(
                                                "consolidationId",
                                                MISSING_CLOSE_ID.toString())
                                        .param(
                                                "issueType",
                                                "OTHER_CRITICAL_INCONSISTENCY"))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrl(
                                        "/closes/"
                                                + CLOSE_ID))
                        .andReturn();

        assertThat(
                submissionResult.getFlashMap()
                        .get(
                                "accountingSubmissionSuccessful"))
                .isEqualTo(
                        true);

        assertThat(
                submissionResult.getFlashMap()
                        .get(
                                "accountingSubmissionMessage"))
                .isEqualTo(
                        SUBMITTED_MESSAGE);

        UUID submissionAttemptId =
                flashUuid(
                        submissionResult,
                        "accountingSubmissionAttemptId");

        UUID validationResultId =
                flashUuid(
                        submissionResult,
                        "accountingSubmissionValidationResultId");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + CLOSE_ID)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        SUBMITTED_MESSAGE)))
                .andExpect(
                        content().string(
                                containsString(
                                        submissionAttemptId.toString())))
                .andExpect(
                        content().string(
                                containsString(
                                        validationResultId.toString())))
                .andExpect(
                        content().string(
                                containsString(
                                        "SENT_TO_ACCOUNTING")))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Enviar a contabilidad"))));

        assertThat(
                closeState())
                .isEqualTo(
                        "SENT_TO_ACCOUNTING");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE id = ?
                          AND close_id = ?
                          AND rule_code = 'VR-008'
                          AND outcome = 'SATISFIED'
                          AND consolidation_id = ?
                          AND is_current = TRUE
                        """,
                        validationResultId,
                        CLOSE_ID,
                        CONSOLIDATION_ID))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                          AND close_id = ?
                          AND vr008_result_id = ?
                          AND consolidation_id = ?
                          AND outcome = 'SUCCEEDED'
                        """,
                        submissionAttemptId,
                        CLOSE_ID,
                        validationResultId,
                        CONSOLIDATION_ID))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        WHERE submission_attempt_id = ?
                        """,
                        submissionAttemptId))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND from_state = 'VALIDATED'
                          AND to_state = 'SENT_TO_ACCOUNTING'
                          AND cause_code = 'ACCOUNTING_SUBMISSION_SUCCEEDED'
                          AND validation_result_id = ?
                          AND consolidation_id = ?
                          AND submission_attempt_id = ?
                        """,
                        CLOSE_ID,
                        validationResultId,
                        CONSOLIDATION_ID,
                        submissionAttemptId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE id = ?
                          AND close_id = ?
                          AND is_current = TRUE
                          AND invalidated_at IS NULL
                        """,
                        CONSOLIDATION_ID,
                        CLOSE_ID))
                .isEqualTo(
                        1L);

        mockMvc.perform(
                        post(
                                submissionUrl(
                                        CLOSE_ID))
                                .session(
                                        session)
                                .with(
                                        csrf()))
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre ya fue enviado "
                                                + "a contabilidad.")));

        assertThat(
                attemptCount())
                .isEqualTo(
                        1L);
    }

    @Test
    void persistsRejectionAndRedirectsWithRegisteredCauses()
            throws Exception {

        persistMissingConsolidationFixture();

        MockHttpSession session =
                authenticatedSession();

        MvcResult rejectionResult =
                mockMvc.perform(
                                post(
                                        submissionUrl(
                                                CLOSE_ID))
                                        .session(
                                                session)
                                        .with(
                                                csrf()))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrl(
                                        "/closes/"
                                                + CLOSE_ID))
                        .andReturn();

        assertThat(
                rejectionResult.getFlashMap()
                        .get(
                                "accountingSubmissionSuccessful"))
                .isEqualTo(
                        false);

        assertThat(
                rejectionResult.getFlashMap()
                        .get(
                                "accountingSubmissionMessage"))
                .isEqualTo(
                        REJECTED_MESSAGE);

        assertThat(
                rejectionResult.getFlashMap()
                        .get(
                                "accountingSubmissionIssueTypes"))
                .isEqualTo(
                        List.of(
                                "CONSOLIDATION_MISSING"));

        UUID submissionAttemptId =
                flashUuid(
                        rejectionResult,
                        "accountingSubmissionAttemptId");

        UUID validationResultId =
                flashUuid(
                        rejectionResult,
                        "accountingSubmissionValidationResultId");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + CLOSE_ID)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        REJECTED_MESSAGE)))
                .andExpect(
                        content().string(
                                containsString(
                                        "CONSOLIDATION_MISSING")))
                .andExpect(
                        content().string(
                                containsString(
                                        "BLOCKED")))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Enviar a contabilidad"))));

        assertThat(
                closeState())
                .isEqualTo(
                        "BLOCKED");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE id = ?
                          AND close_id = ?
                          AND rule_code = 'VR-008'
                          AND outcome = 'FAILED'
                          AND consolidation_id IS NULL
                          AND is_current = TRUE
                        """,
                        validationResultId,
                        CLOSE_ID))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.accounting_submission_attempt
                        WHERE id = ?
                          AND close_id = ?
                          AND vr008_result_id = ?
                          AND consolidation_id IS NULL
                          AND outcome = 'REJECTED'
                        """,
                        submissionAttemptId,
                        CLOSE_ID,
                        validationResultId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.submission_attempt_issue
                        WHERE submission_attempt_id = ?
                          AND issue_type = 'CONSOLIDATION_MISSING'
                          AND consolidation_id IS NULL
                        """,
                        submissionAttemptId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND from_state = 'VALIDATED'
                          AND to_state = 'BLOCKED'
                          AND cause_code = 'ACCOUNTING_SUBMISSION_REJECTED'
                          AND validation_result_id = ?
                          AND consolidation_id IS NULL
                          AND submission_attempt_id = ?
                        """,
                        CLOSE_ID,
                        validationResultId,
                        submissionAttemptId))
                .isEqualTo(
                        1L);
    }

    @Test
    void mapsInvalidMissingAndNonSubmittableCloses()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        mockMvc.perform(
                        post(
                                "/closes/not-a-uuid/"
                                        + "submit-to-accounting")
                                .session(
                                        session)
                                .with(
                                        csrf()))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().string(
                                containsString(
                                        "El identificador del cierre "
                                                + "no es válido.")));

        mockMvc.perform(
                        post(
                                submissionUrl(
                                        MISSING_CLOSE_ID))
                                .session(
                                        session)
                                .with(
                                        csrf()))
                .andExpect(
                        status().isNotFound())
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre solicitado no existe.")));

        persistClose(
                "PREPARATION");

        mockMvc.perform(
                        post(
                                submissionUrl(
                                        CLOSE_ID))
                                .session(
                                        session)
                                .with(
                                        csrf()))
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre debe estar validado "
                                                + "antes de enviarlo "
                                                + "a contabilidad.")));

        assertThat(
                attemptCount())
                .isZero();
    }

    private void persistSuccessfulFixture() {
        persistClose(
                "VALIDATED");

        persistEvent();

        persistConsolidation();
    }

    private void persistMissingConsolidationFixture() {
        persistClose(
                "VALIDATED");

        persistEvent();
    }

    private void persistClose(
            String state) {

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
                CLOSE_ID,
                state,
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT));
    }

    private void persistEvent() {
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
                    'Egreso para envío interno a contabilidad',
                    'VALIDATED',
                    FALSE,
                    FALSE,
                    1,
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
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT),
                databaseTimestamp(
                        CREATED_AT));
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
                        CONSOLIDATED_AT));

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
                    1,
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
                databaseTimestamp(
                        CONSOLIDATED_AT));
    }

    private MockHttpSession authenticatedSession()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/login")
                                        .with(
                                                csrf())
                                        .param(
                                                "username",
                                                "responsible")
                                        .param(
                                                "password",
                                                TEST_PASSWORD))
                        .andExpect(
                                status().is3xxRedirection())
                        .andExpect(
                                redirectedUrl(
                                        "/"))
                        .andReturn();

        MockHttpSession session =
                (MockHttpSession) result
                        .getRequest()
                        .getSession(
                                false);

        assertThat(
                session)
                .isNotNull();

        return session;
    }

    private String closeState() {
        return jdbcTemplate.queryForObject(
                """
                SELECT state
                FROM ocv.operational_close
                WHERE id = ?
                """,
                String.class,
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

    private static UUID flashUuid(
            MvcResult result,
            String attributeName) {

        Object value =
                result.getFlashMap()
                        .get(
                                attributeName);

        assertThat(
                value)
                .isInstanceOf(
                        UUID.class);

        return (UUID) value;
    }

    private static String submissionUrl(
            UUID closeId) {

        return "/closes/"
                + closeId
                + "/submit-to-accounting";
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

    private void clearRegisteredSessions() {
        sessionRegistry.getAllPrincipals()
                .forEach(principal ->
                        sessionRegistry
                                .getAllSessions(
                                        principal,
                                        true)
                                .forEach(session ->
                                        sessionRegistry
                                                .removeSessionInformation(
                                                        session
                                                                .getSessionId())));
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