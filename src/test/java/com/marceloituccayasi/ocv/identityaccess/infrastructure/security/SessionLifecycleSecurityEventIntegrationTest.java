package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.SecurityEventEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.SecurityEventJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.provisioning.ResponsibleUserProvisioner;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SessionLifecycleSecurityEventIntegrationTest {

    private static final String TEST_PASSWORD =
            "session-lifecycle-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityUserJpaRepository identityUserRepository;

    @Autowired
    private SecurityEventJpaRepository securityEventRepository;

    @Autowired
    private ResponsibleUserProvisioner provisioner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessionRegistry;

    @BeforeEach
    void prepareKnownCredential() {
        clearRegisteredSessions();

        identityUserRepository.deleteAll();
        securityEventRepository.deleteAll();

        provisioner.provision();

        IdentityUserEntity identityUser =
                identityUserRepository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        identityUser.synchronize(
                "responsible",
                "responsible",
                passwordEncoder.encode(TEST_PASSWORD),
                Instant.now());

        identityUserRepository.saveAndFlush(identityUser);
        securityEventRepository.deleteAll();
    }

    @AfterEach
    void clearSessionsAfterTest() {
        clearRegisteredSessions();
    }

    @Test
    void recordsExplicitLogout() throws Exception {
        MockHttpSession session =
                sessionFromSuccessfulLogin();

        securityEventRepository.deleteAll();

        mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertThat(eventTypes())
                .containsExactly(SecurityEventType.LOGOUT);
    }

    @Test
    void recordsPreviousSessionReplacement() throws Exception {
        MockHttpSession firstSession =
                sessionFromSuccessfulLogin();

        sessionFromSuccessfulLogin();

        securityEventRepository.deleteAll();

        mockMvc.perform(get("/").session(firstSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?replaced"));

        assertThat(eventTypes())
                .containsExactly(
                        SecurityEventType.SESSION_REPLACED);
    }

    @Test
    void recordsInvalidExpiredSessionIdentifier()
            throws Exception {

        securityEventRepository.deleteAll();

        mockMvc.perform(get("/")
                        .with(request -> {
                            request.setRequestedSessionId(
                                    "expired-session-id");
                            request.setRequestedSessionIdValid(false);
                            return request;
                        }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        List<SecurityEventEntity> events =
                securityEventRepository
                        .findAllByOrderByOccurredAtAsc();

        assertThat(events).hasSize(1);

        SecurityEventEntity event = events.getFirst();

        assertThat(event.eventType())
                .isEqualTo(SecurityEventType.SESSION_EXPIRED);
        assertThat(event.userId()).isNull();
        assertThat(event.usernameNormalized()).isNull();
        assertThat(event.detail())
                .isEqualTo(
                        "Expired session identifier presented.");
    }

    private MockHttpSession sessionFromSuccessfulLogin()
            throws Exception {

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "responsible")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result
                        .getRequest()
                        .getSession(false);

        assertThat(session).isNotNull();
        return session;
    }

    private List<SecurityEventType> eventTypes() {
        return securityEventRepository
                .findAllByOrderByOccurredAtAsc()
                .stream()
                .map(SecurityEventEntity::eventType)
                .toList();
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
