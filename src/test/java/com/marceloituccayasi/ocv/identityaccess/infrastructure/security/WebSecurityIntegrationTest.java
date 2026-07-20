package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
class WebSecurityIntegrationTest {

    private static final String TEST_PASSWORD =
            "test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityUserJpaRepository repository;

    @Autowired
    private ResponsibleUserProvisioner provisioner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessionRegistry;

    @BeforeEach
    void prepareKnownTestCredential() {
        clearRegisteredSessions();
        provisioner.provision();

        IdentityUserEntity identityUser =
                repository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        identityUser.synchronize(
                "responsible",
                "responsible",
                passwordEncoder.encode(TEST_PASSWORD),
                Instant.now());

        repository.saveAndFlush(identityUser);
    }

    @AfterEach
    void clearSessionsAfterTest() {
        clearRegisteredSessions();
    }

    @Test
    void redirectsUnauthenticatedRequestsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(unauthenticated());
    }

    @Test
    void exposesPublicLoginPageWithCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("name=\"_csrf\"")))
                .andExpect(unauthenticated());
    }

    @Test
    void rejectsLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "responsible")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    void authenticatesNormalizedConfiguredUsername() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "  RESPONSIBLE  ")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated()
                        .withUsername("responsible"));
    }

    @Test
    void rejectsInvalidCredentialsUniformly() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "responsible")
                        .param("password", "incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void secondLoginExpiresFirstSession() throws Exception {
        MockHttpSession firstSession =
                sessionFromSuccessfulLogin();

        MockHttpSession secondSession =
                sessionFromSuccessfulLogin();

        assertThat(firstSession.getId())
                .isNotEqualTo(secondSession.getId());

        mockMvc.perform(get("/").session(firstSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?replaced"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(authenticated()
                        .withUsername("responsible"));
    }

    @Test
    void logoutRequiresPostAndCsrf() throws Exception {
        MockHttpSession session =
                sessionFromSuccessfulLogin();

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().isNotFound())
                .andExpect(authenticated());

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(authenticated());

        mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
    }

    private MockHttpSession sessionFromSuccessfulLogin()
            throws Exception {

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "responsible")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated()
                        .withUsername("responsible"))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result
                        .getRequest()
                        .getSession(false);

        assertThat(session).isNotNull();
        return session;
    }

    private void clearRegisteredSessions() {
        sessionRegistry.getAllPrincipals()
                .forEach(principal ->
                        sessionRegistry
                                .getAllSessions(principal, true)
                                .forEach(session ->
                                        sessionRegistry
                                                .removeSessionInformation(
                                                        session.getSessionId())));
    }

}
