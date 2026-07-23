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
import java.sql.Timestamp;
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
class OperationalEventRevisionWebIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

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
    void displaysPreloadedFormAndUpdatesEventRevision()
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
                        "Caja principal",
                        "Ingreso inicial");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + eventId
                                        + "/edit")
                                .session(session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Modificar evento operativo")))
                .andExpect(
                        content().string(
                                containsString(
                                        "125.5000")))
                .andExpect(
                        content().string(
                                containsString(
                                        "Ingreso inicial")))
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"_csrf\"")));

        reviseEvent(
                session,
                closeId,
                eventId,
                "INCOME",
                "175.7500",
                null,
                "2026-07-22T16:00:00Z",
                "Caja revisada",
                "Ingreso operativo revisado")
                .andExpect(
                        status().isSeeOther())
                .andExpect(
                        redirectedUrl(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + eventId));

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
                                        "175.7500")))
                .andExpect(
                        content().string(
                                containsString(
                                        "Ingreso operativo revisado")))
                .andExpect(
                        content().string(
                                containsString(
                                        "Caja revisada")));

        EventSnapshot snapshot =
                readEvent(
                        eventId);

        assertThat(snapshot.eventType())
                .isEqualTo("INCOME");

        assertThat(snapshot.amount())
                .isEqualByComparingTo(
                        "175.7500");

        assertThat(snapshot.balanceEffect())
                .isEqualByComparingTo(
                        "175.7500");

        assertThat(snapshot.reversedEventId())
                .isNull();

        assertThat(snapshot.responsibleName())
                .isEqualTo(
                        "Caja revisada");

        assertThat(snapshot.description())
                .isEqualTo(
                        "Ingreso operativo revisado");

        assertThat(snapshot.state())
                .isEqualTo(
                        "REGISTERED");

        assertThat(snapshot.dataRevision())
                .isEqualTo(2L);

        assertThat(snapshot.updatedByUserId())
                .isEqualTo(
                        "responsible-user");

        assertThat(snapshot.updatedAt())
                .isAfter(
                        snapshot.createdAt());

        assertThat(
                countTransitionsForEvent(
                        eventId))
                .isEqualTo(1L);
    }

    @Test
    void recalculatesDependentCancellationWhenOriginalIsUpdated()
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
                        "Caja principal",
                        "Gasto original");

        UUID cancellationId =
                createEventAndGetId(
                        session,
                        closeId,
                        "CANCELLATION",
                        "80.0000",
                        originalEventId,
                        "2026-07-22T16:05:00Z",
                        "Caja principal",
                        "Anulación inicial");

        reviseEvent(
                session,
                closeId,
                originalEventId,
                "EXPENSE",
                "125.2500",
                null,
                "2026-07-22T16:10:00Z",
                "Caja revisada",
                "Gasto revisado")
                .andExpect(
                        status().isSeeOther())
                .andExpect(
                        redirectedUrl(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + originalEventId));

        EventSnapshot revisedOriginal =
                readEvent(
                        originalEventId);

        EventSnapshot revisedCancellation =
                readEvent(
                        cancellationId);

        assertThat(revisedOriginal.eventType())
                .isEqualTo("EXPENSE");

        assertThat(revisedOriginal.amount())
                .isEqualByComparingTo(
                        "125.2500");

        assertThat(revisedOriginal.balanceEffect())
                .isEqualByComparingTo(
                        "-125.2500");

        assertThat(revisedOriginal.dataRevision())
                .isEqualTo(2L);

        assertThat(revisedCancellation.eventType())
                .isEqualTo(
                        "CANCELLATION");

        assertThat(revisedCancellation.reversedEventId())
                .isEqualTo(
                        originalEventId);

        assertThat(revisedCancellation.amount())
                .isEqualByComparingTo(
                        "125.2500");

        assertThat(revisedCancellation.balanceEffect())
                .isEqualByComparingTo(
                        "125.2500");

        assertThat(revisedCancellation.dataRevision())
                .isEqualTo(2L);

        assertThat(revisedCancellation.updatedByUserId())
                .isEqualTo(
                        "responsible-user");

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + cancellationId)
                                .session(session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "CANCELLATION")))
                .andExpect(
                        content().string(
                                containsString(
                                        "125.2500")))
                .andExpect(
                        content().string(
                                containsString(
                                        originalEventId.toString())));

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
    void rejectsUpdateWhenCloseWasSentToAccounting()
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
                        "DISCOUNT",
                        "30.0000",
                        null,
                        "2026-07-22T17:00:00Z",
                        "Caja principal",
                        "Descuento original");

        jdbcTemplate.update(
                """
                UPDATE ocv.operational_close
                SET state = 'SENT_TO_ACCOUNTING'
                WHERE id = ?
                """,
                closeId);

        reviseEvent(
                session,
                closeId,
                eventId,
                "DISCOUNT",
                "45.0000",
                null,
                "2026-07-22T17:10:00Z",
                "Caja revisada",
                "Descuento rechazado")
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre ya no permite modificar eventos.")));

        EventSnapshot persistedEvent =
                readEvent(
                        eventId);

        assertThat(persistedEvent.amount())
                .isEqualByComparingTo(
                        "30.0000");

        assertThat(persistedEvent.balanceEffect())
                .isEqualByComparingTo(
                        "-30.0000");

        assertThat(persistedEvent.description())
                .isEqualTo(
                        "Descuento original");

        assertThat(persistedEvent.dataRevision())
                .isEqualTo(1L);

        assertThat(
                countTransitionsForEvent(
                        eventId))
                .isEqualTo(1L);
    }

    private UUID createCloseAndGetId(
            MockHttpSession session,
            String periodStart,
            String periodEnd)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
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

    private UUID createEventAndGetId(
            MockHttpSession session,
            UUID closeId,
            String eventType,
            String amount,
            UUID reversedEventId,
            String occurredAt,
            String responsibleName,
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
                        responsibleName,
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

    private ResultActions createEvent(
            MockHttpSession session,
            UUID closeId,
            String eventType,
            String amount,
            UUID reversedEventId,
            String occurredAt,
            String responsibleName,
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
                                responsibleName)
                        .param(
                                "description",
                                description));
    }

    private ResultActions reviseEvent(
            MockHttpSession session,
            UUID closeId,
            UUID eventId,
            String eventType,
            String amount,
            UUID reversedEventId,
            String occurredAt,
            String responsibleName,
            String description)
            throws Exception {

        return mockMvc.perform(
                post(
                        "/closes/"
                                + closeId
                                + "/events/"
                                + eventId
                                + "/edit")
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
                                responsibleName)
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

    private EventSnapshot readEvent(
            UUID eventId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT
                    event_type,
                    amount,
                    balance_effect,
                    reversed_event_id,
                    responsible_name,
                    description,
                    state,
                    data_revision,
                    created_at,
                    updated_at,
                    updated_by_user_id
                FROM ocv.operational_event
                WHERE id = ?
                """,
                (resultSet, rowNumber) ->
                        new EventSnapshot(
                                resultSet.getString(
                                        "event_type"),
                                resultSet.getBigDecimal(
                                        "amount"),
                                resultSet.getBigDecimal(
                                        "balance_effect"),
                                resultSet.getObject(
                                        "reversed_event_id",
                                        UUID.class),
                                resultSet.getString(
                                        "responsible_name"),
                                resultSet.getString(
                                        "description"),
                                resultSet.getString(
                                        "state"),
                                resultSet.getLong(
                                        "data_revision"),
                                toInstant(
                                        resultSet.getTimestamp(
                                                "created_at")),
                                toInstant(
                                        resultSet.getTimestamp(
                                                "updated_at")),
                                resultSet.getString(
                                        "updated_by_user_id")),
                eventId);
    }

    private Long countTransitionsForEvent(
            UUID eventId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ocv.event_state_transition
                WHERE event_id = ?
                """,
                Long.class,
                eventId);
    }

    private Long countRows(
            String qualifiedTableName) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM "
                        + qualifiedTableName,
                Long.class);
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

    private static Instant toInstant(
            Timestamp timestamp) {

        return timestamp == null
                ? null
                : timestamp.toInstant();
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

    private record EventSnapshot(
            String eventType,
            BigDecimal amount,
            BigDecimal balanceEffect,
            UUID reversedEventId,
            String responsibleName,
            String description,
            String state,
            long dataRevision,
            Instant createdAt,
            Instant updatedAt,
            String updatedByUserId) {
    }

}