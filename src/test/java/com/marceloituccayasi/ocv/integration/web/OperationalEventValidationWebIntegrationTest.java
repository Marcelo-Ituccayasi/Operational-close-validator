package com.marceloituccayasi.ocv.integration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.test.web.servlet.ResultActions;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.provisioning.ResponsibleUserProvisioner;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OperationalEventValidationWebIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

    private static final String FAILED_MESSAGE =
            "La validación finalizó con reglas fallidas. "
                    + "Revisa el estado del evento y sus alertas.";

    private static final String VALIDATED_MESSAGE =
            "El evento fue validado correctamente.";

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
    void failsIncomeValidationAndResolvesAlertAfterMatchingEvidence()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session);

        UUID eventId =
                createIncomeAndGetId(
                        session,
                        closeId);

        String eventDetailUrl =
                "/closes/"
                        + closeId
                        + "/events/"
                        + eventId;

        mockMvc.perform(
                        get(
                                eventDetailUrl)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Validar evento operativo")))
                .andExpect(
                        content().string(
                                containsString(
                                        "REGISTERED")));

        MvcResult failedValidation =
                validateEvent(
                        session,
                        closeId,
                        eventId)
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrl(
                                        eventDetailUrl))
                        .andReturn();

        assertThat(
                failedValidation.getFlashMap()
                        .get(
                                "validationSuccessful"))
                .isEqualTo(
                        false);

        assertThat(
                failedValidation.getFlashMap()
                        .get(
                                "validationMessage"))
                .isEqualTo(
                        FAILED_MESSAGE);

        mockMvc.perform(
                        get(
                                eventDetailUrl)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        FAILED_MESSAGE)))
                .andExpect(
                        content().string(
                                containsString(
                                        "OBSERVED")));

        assertThat(
                eventState(
                        eventId))
                .isEqualTo(
                        "OBSERVED");

        assertThat(
                closeState(
                        closeId))
                .isEqualTo(
                        "PREPARATION");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE event_id = ?
                          AND rule_code = 'VR-002'
                          AND outcome = 'FAILED'
                          AND is_current = TRUE
                          AND event_data_revision = 1
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert
                        WHERE event_id = ?
                          AND cause_code = 'VR-002'
                          AND severity = 'CRITICAL'
                          AND is_blocking = TRUE
                          AND state = 'ACTIVE'
                          AND source_validation_result_id IS NOT NULL
                          AND resolved_by_validation_result_id IS NULL
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert_transition transition
                        JOIN ocv.alert alert
                          ON alert.id = transition.alert_id
                        WHERE alert.event_id = ?
                          AND alert.cause_code = 'VR-002'
                          AND transition.from_state IS NULL
                          AND transition.to_state = 'ACTIVE'
                          AND transition.validation_result_id IS NULL
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        createMatchingEvidence(
                session,
                closeId,
                eventId)
                .andExpect(
                        status().isSeeOther())
                .andExpect(
                        redirectedUrl(
                                eventDetailUrl));

        assertThat(
                eventDataRevision(
                        eventId))
                .isEqualTo(
                        2L);

        assertThat(
                eventState(
                        eventId))
                .isEqualTo(
                        "OBSERVED");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE event_id = ?
                          AND rule_code = 'VR-002'
                          AND outcome = 'FAILED'
                          AND is_current = FALSE
                          AND invalidated_at IS NOT NULL
                          AND invalidation_reason =
                              'Operational Event data revision changed.'
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE event_id = ?
                          AND is_current = TRUE
                        """,
                        eventId))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert
                        WHERE event_id = ?
                          AND cause_code = 'VR-002'
                          AND state = 'ACTIVE'
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        MvcResult successfulValidation =
                validateEvent(
                        session,
                        closeId,
                        eventId)
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrl(
                                        eventDetailUrl))
                        .andReturn();

        assertThat(
                successfulValidation.getFlashMap()
                        .get(
                                "validationSuccessful"))
                .isEqualTo(
                        true);

        assertThat(
                successfulValidation.getFlashMap()
                        .get(
                                "validationMessage"))
                .isEqualTo(
                        VALIDATED_MESSAGE);

        mockMvc.perform(
                        get(
                                eventDetailUrl)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        VALIDATED_MESSAGE)))
                .andExpect(
                        content().string(
                                containsString(
                                        "VALIDATED")));

        assertThat(
                eventState(
                        eventId))
                .isEqualTo(
                        "VALIDATED");

        assertThat(
                eventDataRevision(
                        eventId))
                .isEqualTo(
                        2L);

        assertThat(
                closeState(
                        closeId))
                .isEqualTo(
                        "PREPARATION");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE event_id = ?
                          AND rule_code = 'VR-002'
                          AND outcome = 'SATISFIED'
                          AND is_current = TRUE
                          AND event_data_revision = 2
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.validation_result
                        WHERE event_id = ?
                          AND rule_code = 'VR-002'
                        """,
                        eventId))
                .isEqualTo(
                        2L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert alert
                        JOIN ocv.validation_result result
                          ON result.id =
                             alert.resolved_by_validation_result_id
                        WHERE alert.event_id = ?
                          AND alert.cause_code = 'VR-002'
                          AND alert.state = 'RESOLVED'
                          AND alert.closed_at IS NOT NULL
                          AND result.rule_code = 'VR-002'
                          AND result.outcome = 'SATISFIED'
                          AND result.is_current = TRUE
                          AND result.event_data_revision = 2
                        """,
                        eventId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.alert_transition transition
                        JOIN ocv.alert alert
                          ON alert.id = transition.alert_id
                        WHERE alert.event_id = ?
                          AND alert.cause_code = 'VR-002'
                          AND (
                              (
                                  transition.from_state IS NULL
                                  AND transition.to_state = 'ACTIVE'
                              )
                              OR
                              (
                                  transition.from_state = 'ACTIVE'
                                  AND transition.to_state = 'RESOLVED'
                                  AND transition.validation_result_id IS NOT NULL
                              )
                          )
                        """,
                        eventId))
                .isEqualTo(
                        2L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_state_transition
                        WHERE event_id = ?
                          AND cause_code = 'EVENT_VALIDATION_APPLIED'
                        """,
                        eventId))
                .isEqualTo(
                        2L);
    }

    private UUID createCloseAndGetId(
            MockHttpSession session)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/closes")
                                        .session(
                                                session)
                                        .with(
                                                csrf())
                                        .param(
                                                "periodStart",
                                                "2026-07-01")
                                        .param(
                                                "periodEnd",
                                                "2026-07-31")
                                        .param(
                                                "currencyCode",
                                                "PEN")
                                        .param(
                                                "initialBalance",
                                                "1250.5000"))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrlPattern(
                                        "/closes/*"))
                        .andReturn();

        return lastIdentifierFromRedirect(
                result);
    }

    private UUID createIncomeAndGetId(
            MockHttpSession session,
            UUID closeId)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/closes/"
                                                + closeId
                                                + "/events")
                                        .session(
                                                session)
                                        .with(
                                                csrf())
                                        .param(
                                                "eventType",
                                                "INCOME")
                                        .param(
                                                "amount",
                                                "125.5000")
                                        .param(
                                                "reversedEventId",
                                                "")
                                        .param(
                                                "occurredAt",
                                                "2026-07-22T15:30:00Z")
                                        .param(
                                                "responsibleName",
                                                "Caja principal")
                                        .param(
                                                "description",
                                                "Ingreso sujeto a validación"))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrlPattern(
                                        "/closes/*/events/*"))
                        .andReturn();

        return lastIdentifierFromRedirect(
                result);
    }

    private ResultActions validateEvent(
            MockHttpSession session,
            UUID closeId,
            UUID eventId)
            throws Exception {

        return mockMvc.perform(
                post(
                        "/closes/"
                                + closeId
                                + "/events/"
                                + eventId
                                + "/validate")
                        .session(
                                session)
                        .with(
                                csrf()));
    }

    private ResultActions createMatchingEvidence(
            MockHttpSession session,
            UUID closeId,
            UUID eventId)
            throws Exception {

        return mockMvc.perform(
                post(
                        "/closes/"
                                + closeId
                                + "/events/"
                                + eventId
                                + "/supporting-evidence")
                        .session(
                                session)
                        .with(
                                csrf())
                        .param(
                                "evidenceType",
                                "RECEIPT")
                        .param(
                                "contentReference",
                                "income-125-5000")
                        .param(
                                "supportedAmount",
                                "125.5000")
                        .param(
                                "evidenceDate",
                                "2026-07-22")
                        .param(
                                "legibilityStatus",
                                "LEGIBLE"));
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
                                redirectedUrl("/"))
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

    private String eventState(
            UUID eventId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT state
                FROM ocv.operational_event
                WHERE id = ?
                """,
                String.class,
                eventId);
    }

    private Long eventDataRevision(
            UUID eventId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT data_revision
                FROM ocv.operational_event
                WHERE id = ?
                """,
                Long.class,
                eventId);
    }

    private String closeState(
            UUID closeId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT state
                FROM ocv.operational_close
                WHERE id = ?
                """,
                String.class,
                closeId);
    }

    private Long count(
            String sql,
            Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                arguments);
    }

    private static UUID lastIdentifierFromRedirect(
            MvcResult result) {

        String redirectedUrl =
                result.getResponse()
                        .getRedirectedUrl();

        assertThat(
                redirectedUrl)
                .isNotBlank();

        int lastSeparator =
                redirectedUrl.lastIndexOf(
                        '/');

        return UUID.fromString(
                redirectedUrl.substring(
                        lastSeparator + 1));
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

}