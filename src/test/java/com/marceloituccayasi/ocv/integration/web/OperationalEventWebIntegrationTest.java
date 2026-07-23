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
class OperationalEventWebIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

    private static final UUID MISSING_CLOSE_ID =
            UUID.fromString(
                    "e28b4687-9c93-4d71-bd63-683edc100001");

    private static final UUID MISSING_EVENT_ID =
            UUID.fromString(
                    "e28b4687-9c93-4d71-bd63-683edc100002");

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
    void protectsRoutesAndDisplaysCreationFormWithCsrf()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + MISSING_CLOSE_ID
                                        + "/events"))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/login"));

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/new")
                                .session(session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Registrar evento operativo")))
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"_csrf\"")));
    }

    @Test
    void createsPersistsDisplaysAndListsIncome()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        UUID eventId =
                createEventAndGetId(
                        session,
                        closeId,
                        "INCOME",
                        "125.5000",
                        null,
                        "2026-07-22T15:30:00Z",
                        "Ingreso de venta");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + eventId)
                                .session(session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Detalle del evento operativo")))
                .andExpect(
                        content().string(
                                containsString("INCOME")))
                .andExpect(
                        content().string(
                                containsString("125.5000")))
                .andExpect(
                        content().string(
                                containsString("REGISTERED")))
                .andExpect(
                        content().string(
                                containsString(
                                        "Ingreso de venta")));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events")
                                .session(session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Eventos operativos")))
                .andExpect(
                        content().string(
                                containsString("INCOME")))
                .andExpect(
                        content().string(
                                containsString("125.5000")))
                .andExpect(
                        content().string(
                                containsString(
                                        eventId.toString())));

        Long persistedEventRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.operational_event
                        WHERE id = ?
                          AND close_id = ?
                          AND event_type = 'INCOME'
                          AND amount = 125.5000
                          AND balance_effect = 125.5000
                          AND state = 'REGISTERED'
                          AND data_revision = 1
                          AND evidence_required = FALSE
                          AND authorization_required = FALSE
                          AND created_by_user_id = 'responsible-user'
                        """,
                        Long.class,
                        eventId,
                        closeId);

        Long persistedTransitionRows =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM ocv.event_state_transition
                        WHERE event_id = ?
                          AND from_state IS NULL
                          AND to_state = 'REGISTERED'
                          AND cause_code = 'EVENT_CREATED'
                          AND actor_user_id = 'responsible-user'
                        """,
                        Long.class,
                        eventId);

        assertThat(persistedEventRows)
                .isEqualTo(1L);

        assertThat(persistedTransitionRows)
                .isEqualTo(1L);
    }

    @Test
    void createsCancellationAndRejectsDuplicateCancellation()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        UUID originalEventId =
                createEventAndGetId(
                        session,
                        closeId,
                        "EXPENSE",
                        "80.0000",
                        null,
                        "2026-07-22T16:00:00Z",
                        "Gasto original");

        UUID cancellationId =
                createEventAndGetId(
                        session,
                        closeId,
                        "CANCELLATION",
                        "80.0000",
                        originalEventId,
                        "2026-07-22T16:05:00Z",
                        "Anulación del gasto");

        BigDecimal originalEffect =
                jdbcTemplate.queryForObject(
                        """
                        SELECT balance_effect
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        BigDecimal.class,
                        originalEventId);

        BigDecimal cancellationEffect =
                jdbcTemplate.queryForObject(
                        """
                        SELECT balance_effect
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        BigDecimal.class,
                        cancellationId);

        UUID persistedReversedEventId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT reversed_event_id
                        FROM ocv.operational_event
                        WHERE id = ?
                        """,
                        UUID.class,
                        cancellationId);

        assertThat(originalEffect)
                .isEqualByComparingTo("-80.0000");

        assertThat(cancellationEffect)
                .isEqualByComparingTo("80.0000");

        assertThat(persistedReversedEventId)
                .isEqualTo(originalEventId);

        createEvent(
                session,
                closeId,
                "CANCELLATION",
                "80.0000",
                originalEventId,
                "2026-07-22T16:10:00Z",
                "Segunda anulación")
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "ya tiene una anulación")));

        assertThat(
                countRows(
                        "ocv.operational_event"))
                .isEqualTo(2L);

        assertThat(
                countRows(
                        "ocv.event_state_transition"))
                .isEqualTo(2L);
    }

    @Test
    void rejectsCancellationAcrossDifferentCloses()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID firstCloseId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        UUID secondCloseId =
                createCloseAndGetId(
                        session,
                        "2026-08-01",
                        "2026-08-31");

        UUID originalEventId =
                createEventAndGetId(
                        session,
                        firstCloseId,
                        "EXPENSE",
                        "50.0000",
                        null,
                        "2026-07-22T17:00:00Z",
                        "Evento del primer cierre");

        createEvent(
                session,
                secondCloseId,
                "CANCELLATION",
                "50.0000",
                originalEventId,
                "2026-08-22T17:05:00Z",
                "Anulación cruzada")
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().string(
                                containsString(
                                        "Los datos del evento no son válidos.")));

        assertThat(
                countRows(
                        "ocv.operational_close"))
                .isEqualTo(2L);

        assertThat(
                countRows(
                        "ocv.operational_event"))
                .isEqualTo(1L);
    }

    @Test
    void rejectsCreationWhenCloseWasSentToAccounting()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        jdbcTemplate.update(
                """
                UPDATE ocv.operational_close
                SET state = 'SENT_TO_ACCOUNTING'
                WHERE id = ?
                """,
                closeId);

        createEvent(
                session,
                closeId,
                "INCOME",
                "25.0000",
                null,
                "2026-07-22T18:00:00Z",
                "Ingreso rechazado")
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "ya no permite registrar eventos")));

        assertThat(
                countRows(
                        "ocv.operational_event"))
                .isZero();

        assertThat(
                countRows(
                        "ocv.event_state_transition"))
                .isZero();
    }

    @Test
    void rejectsMalformedEventInputWithoutPersisting()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        createEvent(
                session,
                closeId,
                "INCOME",
                "not-a-number",
                null,
                "not-an-instant",
                "Evento inválido")
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().string(
                                containsString(
                                        "Los datos ingresados no son válidos.")));

        assertThat(
                countRows(
                        "ocv.operational_event"))
                .isZero();
    }

    @Test
    void distinguishesInvalidAndMissingIdentifiers()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        mockMvc.perform(
                        get(
                                "/closes/not-a-uuid/events")
                                .session(session))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().string(
                                containsString(
                                        "El identificador del cierre no es válido.")));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + MISSING_CLOSE_ID
                                        + "/events")
                                .session(session))
                .andExpect(
                        status().isNotFound())
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre solicitado no existe.")));

        UUID closeId =
                createCloseAndGetId(
                        session,
                        "2026-07-01",
                        "2026-07-31");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/not-a-uuid")
                                .session(session))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().string(
                                containsString(
                                        "El identificador del evento no es válido.")));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + MISSING_EVENT_ID)
                                .session(session))
                .andExpect(
                        status().isNotFound())
                .andExpect(
                        content().string(
                                containsString(
                                        "El evento solicitado no existe dentro de este cierre.")));
    }

    private UUID createCloseAndGetId(
            MockHttpSession session,
            String periodStart,
            String periodEnd)
            throws Exception {

        MvcResult result =
                createClose(
                        session,
                        periodStart,
                        periodEnd)
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrlPattern(
                                        "/closes/*"))
                        .andReturn();

        return lastIdentifierFromRedirect(
                result);
    }

    private UUID createEventAndGetId(
            MockHttpSession session,
            UUID closeId,
            String eventType,
            String amount,
            UUID reversedEventId,
            String occurredAt,
            String description)
            throws Exception {

        MvcResult result =
                createEvent(
                        session,
                        closeId,
                        eventType,
                        amount,
                        reversedEventId,
                        occurredAt,
                        description)
                        .andExpect(
                                status().isSeeOther())
                        .andExpect(
                                redirectedUrlPattern(
                                        "/closes/*/events/*"))
                        .andReturn();

        return lastIdentifierFromRedirect(
                result);
    }

    private ResultActions createClose(
            MockHttpSession session,
            String periodStart,
            String periodEnd)
            throws Exception {

        return mockMvc.perform(
                post("/closes")
                        .session(session)
                        .with(csrf())
                        .param(
                                "periodStart",
                                periodStart)
                        .param(
                                "periodEnd",
                                periodEnd)
                        .param(
                                "currencyCode",
                                "PEN")
                        .param(
                                "initialBalance",
                                "1250.5000"));
    }

    private ResultActions createEvent(
            MockHttpSession session,
            UUID closeId,
            String eventType,
            String amount,
            UUID reversedEventId,
            String occurredAt,
            String description)
            throws Exception {

        return mockMvc.perform(
                post(
                        "/closes/"
                                + closeId
                                + "/events")
                        .session(session)
                        .with(csrf())
                        .param(
                                "eventType",
                                eventType)
                        .param(
                                "amount",
                                amount)
                        .param(
                                "reversedEventId",
                                reversedEventId == null
                                        ? ""
                                        : reversedEventId.toString())
                        .param(
                                "occurredAt",
                                occurredAt)
                        .param(
                                "responsibleName",
                                "Caja principal")
                        .param(
                                "description",
                                description));
    }

    private MockHttpSession authenticatedSession()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/login")
                                        .with(csrf())
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
                        .getSession(false);

        assertThat(session)
                .isNotNull();

        return session;
    }

    private static UUID lastIdentifierFromRedirect(
            MvcResult result) {

        String redirectedUrl =
                result.getResponse()
                        .getRedirectedUrl();

        assertThat(redirectedUrl)
                .isNotBlank();

        int lastSeparator =
                redirectedUrl.lastIndexOf('/');

        return UUID.fromString(
                redirectedUrl.substring(
                        lastSeparator + 1));
    }

    private void cleanOperationalCloseTables() {
        jdbcTemplate.execute(
                """
                TRUNCATE TABLE
                    ocv.event_state_transition,
                    ocv.operational_event,
                    ocv.close_state_transition,
                    ocv.operational_close
                """);
    }

    private Long countRows(
            String qualifiedTableName) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + qualifiedTableName,
                Long.class);
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