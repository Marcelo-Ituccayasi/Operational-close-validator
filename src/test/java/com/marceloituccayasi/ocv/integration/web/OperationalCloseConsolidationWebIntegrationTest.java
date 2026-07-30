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

import java.math.BigDecimal;
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
class OperationalCloseConsolidationWebIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

    private static final String CONSOLIDATED_MESSAGE =
            "El cierre fue consolidado correctamente.";

    private static final String REJECTED_MESSAGE =
            "La consolidación fue rechazada. Revisa los eventos, "
                    + "resultados de validación y alertas bloqueantes.";

    private static final UUID MISSING_CLOSE_ID =
            UUID.fromString(
                    "716cb0ca-2af3-4a85-98ac-800000000001");

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
    void protectsConsolidationRoutesAndRequiresCsrf()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + MISSING_CLOSE_ID
                                        + "/consolidate"))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login"));

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session);

        mockMvc.perform(
                        post(
                                "/closes/"
                                        + closeId
                                        + "/consolidate")
                                .session(
                                        session)
                                .param(
                                        "actualBalance",
                                        "1250.5000"))
                .andExpect(
                        status().isForbidden());

        assertThat(
                closeState(
                        closeId))
                .isEqualTo(
                        "PREPARATION");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE close_id = ?
                        """,
                        closeId))
                .isZero();
    }

    @Test
    void rendersPreviewAndCompletesConsolidation()
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

        createMatchingEvidence(
                session,
                closeId,
                eventId)
                .andExpect(
                        status().isSeeOther());

        validateEvent(
                session,
                closeId,
                eventId)
                .andExpect(
                        status().isSeeOther());

        assertThat(
                eventState(
                        eventId))
                .isEqualTo(
                        "VALIDATED");

        String consolidationUrl =
                "/closes/"
                        + closeId
                        + "/consolidate";

        mockMvc.perform(
                        get(
                                consolidationUrl)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Consolidar cierre operativo")))
                .andExpect(
                        content().string(
                                containsString(
                                        eventId.toString())))
                .andExpect(
                        content().string(
                                containsString(
                                        "1250.5000")))
                .andExpect(
                        content().string(
                                containsString(
                                        "125.5000")))
                .andExpect(
                        content().string(
                                containsString(
                                        "1376.0000")))
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre cumple actualmente "
                                                + "las precondiciones")))
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"_csrf\"")));

        MvcResult completionResult =
                mockMvc.perform(
                                post(
                                        consolidationUrl)
                                        .session(
                                                session)
                                        .with(
                                                csrf())
                                        .param(
                                                "actualBalance",
                                                "1376.0000"))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrl(
                                        "/closes/"
                                                + closeId))
                        .andReturn();

        assertThat(
                completionResult.getFlashMap()
                        .get(
                                "consolidationSuccessful"))
                .isEqualTo(
                        true);

        assertThat(
                completionResult.getFlashMap()
                        .get(
                                "consolidationMessage"))
                .isEqualTo(
                        CONSOLIDATED_MESSAGE);

        Object consolidationIdentifier =
                completionResult.getFlashMap()
                        .get(
                                "consolidationId");

        assertThat(
                consolidationIdentifier)
                .isInstanceOf(
                        UUID.class);

        UUID consolidationId =
                (UUID) consolidationIdentifier;

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        CONSOLIDATED_MESSAGE)))
                .andExpect(
                        content().string(
                                containsString(
                                        consolidationId.toString())))
                .andExpect(
                        content().string(
                                containsString(
                                        "VALIDATED")));

        assertThat(
                closeState(
                        closeId))
                .isEqualTo(
                        "VALIDATED");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE id = ?
                          AND close_id = ?
                          AND is_current = TRUE
                          AND invalidated_at IS NULL
                          AND invalidation_reason IS NULL
                        """,
                        consolidationId,
                        closeId))
                .isEqualTo(
                        1L);

        assertThat(
                decimalValue(
                        """
                        SELECT initial_balance
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        consolidationId))
                .isEqualByComparingTo(
                        new BigDecimal(
                                "1250.5000"));

        assertThat(
                decimalValue(
                        """
                        SELECT total_income
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        consolidationId))
                .isEqualByComparingTo(
                        new BigDecimal(
                                "125.5000"));

        assertThat(
                decimalValue(
                        """
                        SELECT expected_balance
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        consolidationId))
                .isEqualByComparingTo(
                        new BigDecimal(
                                "1376.0000"));

        assertThat(
                decimalValue(
                        """
                        SELECT actual_balance
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        consolidationId))
                .isEqualByComparingTo(
                        new BigDecimal(
                                "1376.0000"));

        assertThat(
                decimalValue(
                        """
                        SELECT difference
                        FROM ocv.consolidation
                        WHERE id = ?
                        """,
                        consolidationId))
                .isEqualByComparingTo(
                        BigDecimal.ZERO);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation_event_snapshot
                        WHERE consolidation_id = ?
                          AND event_id = ?
                          AND event_data_revision = 2
                          AND event_state = 'VALIDATED'
                        """,
                        consolidationId,
                        eventId))
                .isEqualTo(
                        1L);

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND cause_code = 'CONSOLIDATION_COMPLETED'
                          AND from_state = 'PREPARATION'
                          AND to_state = 'VALIDATED'
                          AND consolidation_id = ?
                        """,
                        closeId,
                        consolidationId))
                .isEqualTo(
                        1L);
    }

    @Test
    void rejectsEmptyCloseAndMovesItToBlocked()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session);

        String consolidationUrl =
                "/closes/"
                        + closeId
                        + "/consolidate";

        mockMvc.perform(
                        get(
                                consolidationUrl)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre todavía no contiene "
                                                + "eventos operativos.")))
                .andExpect(
                        content().string(
                                containsString(
                                        "impedir la consolidación.")));

        MvcResult rejectionResult =
                mockMvc.perform(
                                post(
                                        consolidationUrl)
                                        .session(
                                                session)
                                        .with(
                                                csrf())
                                        .param(
                                                "actualBalance",
                                                "1250.5000"))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrl(
                                        "/closes/"
                                                + closeId))
                        .andReturn();

        assertThat(
                rejectionResult.getFlashMap()
                        .get(
                                "consolidationSuccessful"))
                .isEqualTo(
                        false);

        assertThat(
                rejectionResult.getFlashMap()
                        .get(
                                "consolidationMessage"))
                .isEqualTo(
                        REJECTED_MESSAGE);

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId)
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
                                        "BLOCKED")));

        assertThat(
                closeState(
                        closeId))
                .isEqualTo(
                        "BLOCKED");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE close_id = ?
                        """,
                        closeId))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND cause_code = 'CONSOLIDATION_REJECTED'
                          AND from_state = 'PREPARATION'
                          AND to_state = 'BLOCKED'
                          AND consolidation_id IS NULL
                        """,
                        closeId))
                .isEqualTo(
                        1L);
    }

    @Test
    void rejectsMalformedActualBalanceWithoutChangingClose()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session);

        mockMvc.perform(
                        post(
                                "/closes/"
                                        + closeId
                                        + "/consolidate")
                                .session(
                                        session)
                                .with(
                                        csrf())
                                .param(
                                        "actualBalance",
                                        "not-a-number"))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().string(
                                containsString(
                                        "El saldo real no tiene un "
                                                + "formato decimal válido.")))
                .andExpect(
                        content().string(
                                containsString(
                                        "Consolidar cierre operativo")));

        assertThat(
                closeState(
                        closeId))
                .isEqualTo(
                        "PREPARATION");

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.consolidation
                        WHERE close_id = ?
                        """,
                        closeId))
                .isZero();

        assertThat(
                count(
                        """
                        SELECT COUNT(*)
                        FROM ocv.close_state_transition
                        WHERE close_id = ?
                          AND cause_code IN (
                              'CONSOLIDATION_COMPLETED',
                              'CONSOLIDATION_REJECTED'
                          )
                        """,
                        closeId))
                .isZero();
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
                                                "Ingreso incluido en "
                                                        + "consolidación"))
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrlPattern(
                                        "/closes/*/events/*"))
                        .andReturn();

        return lastIdentifierFromRedirect(
                result);
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
                                "income-consolidation-125-5000")
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

    private BigDecimal decimalValue(
            String sql,
            Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                BigDecimal.class,
                arguments);
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