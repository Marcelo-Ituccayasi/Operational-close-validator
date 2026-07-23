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

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.provisioning.ResponsibleUserProvisioner;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OperationalCloseWebIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

    private static final UUID MISSING_CLOSE_ID =
            UUID.fromString(
                    "d809df30-6598-46f2-b2b6-dd460d97936b");

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
                passwordEncoder.encode(TEST_PASSWORD),
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
    void protectsOperationalCloseRoutes() throws Exception {
        mockMvc.perform(get("/closes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void displaysDashboardAndCreationFormWithCsrf()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        mockMvc.perform(
                        get("/dashboard")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(
                                "Panel de cierres operativos")))
                .andExpect(content().string(
                        containsString("responsible")));

        mockMvc.perform(
                        get("/closes/new")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(
                                "Crear cierre operativo")))
                .andExpect(content().string(
                        containsString(
                                "name=\"_csrf\"")));
    }

    @Test
    void createsPersistsDisplaysAndListsClose()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        MvcResult creationResult =
                createClose(
                        session,
                        "2026-07-01",
                        "2026-07-31")
                        .andExpect(status().isSeeOther())
                        .andExpect(
                                redirectedUrlPattern(
                                        "/closes/*"))
                        .andReturn();

        String detailUrl =
                creationResult
                        .getResponse()
                        .getRedirectedUrl();

        assertThat(detailUrl)
                .isNotBlank();

        mockMvc.perform(
                        get(detailUrl)
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("2026-07-01")))
                .andExpect(content().string(
                        containsString("2026-07-31")))
                .andExpect(content().string(
                        containsString("PEN")))
                .andExpect(content().string(
                        containsString("1250.5000")))
                .andExpect(content().string(
                        containsString("PREPARATION")));

        mockMvc.perform(
                        get("/closes")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("2026-07-01")))
                .andExpect(content().string(
                        containsString("PREPARATION")));

        assertThat(
                countRows("ocv.operational_close"))
                .isEqualTo(1L);

        assertThat(
                countRows(
                        "ocv.close_state_transition"))
                .isEqualTo(1L);
    }

    @Test
    void rejectsMalformedCreationInput()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        mockMvc.perform(
                        post("/closes")
                                .session(session)
                                .with(csrf())
                                .param(
                                        "periodStart",
                                        "not-a-date")
                                .param(
                                        "periodEnd",
                                        "2026-07-31")
                                .param(
                                        "currencyCode",
                                        "PEN")
                                .param(
                                        "initialBalance",
                                        "1250.5000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString(
                                "Los datos ingresados no son válidos.")));

        assertThat(
                countRows("ocv.operational_close"))
                .isZero();
    }

    @Test
    void rejectsDomainInvalidCreationInput()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        createClose(
                session,
                "2026-08-31",
                "2026-08-01")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString(
                                "Los datos del cierre no son válidos.")));

        assertThat(
                countRows("ocv.operational_close"))
                .isZero();
    }

    @Test
    void returnsConflictForDuplicatePeriod()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        createClose(
                session,
                "2026-09-01",
                "2026-09-30")
                .andExpect(status().isSeeOther());

        createClose(
                session,
                "2026-09-01",
                "2026-09-30")
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString(
                                "Ya existe un cierre para el período indicado.")));

        assertThat(
                countRows("ocv.operational_close"))
                .isEqualTo(1L);
    }

    @Test
    void distinguishesInvalidAndMissingCloseIdentifiers()
            throws Exception {

        MockHttpSession session =
                authenticatedSession();

        mockMvc.perform(
                        get("/closes/not-a-uuid")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString(
                                "El identificador del cierre no es válido.")));

        mockMvc.perform(
                        get("/closes/" + MISSING_CLOSE_ID)
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        containsString(
                                "El cierre solicitado no existe.")));
    }

    private org.springframework.test.web.servlet.ResultActions
            createClose(
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
                                status()
                                        .is3xxRedirection())
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
