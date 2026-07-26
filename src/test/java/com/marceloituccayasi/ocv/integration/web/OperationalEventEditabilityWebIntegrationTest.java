package com.marceloituccayasi.ocv.integration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class OperationalEventEditabilityWebIntegrationTest {

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
                                IdentityUserEntity
                                        .RESPONSIBLE_USER_ID)
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
    void displaysMutationLinksWhileCloseIsEditable()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session);

        UUID eventId =
                createEventAndGetId(
                        session,
                        closeId);

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events")
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Registrar evento operativo")))
                .andExpect(
                        content().string(
                                containsString(
                                        "PREPARATION")));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + eventId)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Modificar evento operativo")))
                .andExpect(
                        content().string(
                                containsString(
                                        "PREPARATION")));
    }

    @Test
    void hidesMutationLinksAndRejectsDirectFormsAfterSubmission()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        UUID closeId =
                createCloseAndGetId(
                        session);

        UUID eventId =
                createEventAndGetId(
                        session,
                        closeId);

        jdbcTemplate.update(
                """
                UPDATE ocv.operational_close
                SET state = 'SENT_TO_ACCOUNTING'
                WHERE id = ?
                """,
                closeId);

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events")
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "SENT_TO_ACCOUNTING")))
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre fue enviado a contabilidad y ya no admite modificaciones.")))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Registrar evento operativo"))));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + eventId)
                                .session(
                                        session))
                .andExpect(
                        status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "SENT_TO_ACCOUNTING")))
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre fue enviado a contabilidad y ya no admite modificaciones.")))
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Modificar evento operativo"))));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/new")
                                .session(
                                        session))
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "Cierre no editable")))
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre fue enviado a contabilidad y ya no admite modificaciones.")));

        mockMvc.perform(
                        get(
                                "/closes/"
                                        + closeId
                                        + "/events/"
                                        + eventId
                                        + "/edit")
                                .session(
                                        session))
                .andExpect(
                        status().isConflict())
                .andExpect(
                        content().string(
                                containsString(
                                        "Cierre no editable")))
                .andExpect(
                        content().string(
                                containsString(
                                        "El cierre fue enviado a contabilidad y ya no admite modificaciones.")));
    }

    private UUID createCloseAndGetId(
            MockHttpSession session)
            throws Exception {

        MvcResult result =
                createClose(
                        session)
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
            UUID closeId)
            throws Exception {

        MvcResult result =
                createEvent(
                        session,
                        closeId)
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
            MockHttpSession session)
            throws Exception {

        return mockMvc.perform(
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
                                "1250.5000"));
    }

    private ResultActions createEvent(
            MockHttpSession session,
            UUID closeId)
            throws Exception {

        return mockMvc.perform(
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
                                "Ingreso para probar editabilidad"));
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