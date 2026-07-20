package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class AuthenticationSecurityEventIntegrationTest {

    private static final String TEST_PASSWORD =
            "security-event-test-password";

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
    void prepareKnownIdentity() {
        clearRegisteredSessions();
        securityEventRepository.deleteAll();
        identityUserRepository.deleteAll();

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
    }

    @AfterEach
    void clearSessions() {
        clearRegisteredSessions();
    }

    @Test
    void recordsInitialProvisioningWithoutSecrets() {
        List<SecurityEventEntity> events =
                securityEventRepository
                        .findAllByOrderByOccurredAtAsc();

        assertThat(events).hasSize(1);

        SecurityEventEntity event = events.getFirst();

        assertThat(event.eventType())
                .isEqualTo(SecurityEventType.USER_PROVISIONED);
        assertThat(event.userId())
                .isEqualTo(IdentityUserEntity.RESPONSIBLE_USER_ID);
        assertThat(event.usernameNormalized())
                .isEqualTo("responsible");
        assertThat(event.detail())
                .isEqualTo("Responsible user provisioned.")
                .doesNotContain(
                        TEST_PASSWORD,
                        "{bcrypt}",
                        "$2a$");
    }

    @Test
    void recordsFailedLoginWithNormalizedPresentedUsername()
            throws Exception {

        securityEventRepository.deleteAll();

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "  UNKNOWN-USER  ")
                        .param("password", "incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        List<SecurityEventEntity> events =
                securityEventRepository
                        .findAllByOrderByOccurredAtAsc();

        assertThat(events).hasSize(1);

        SecurityEventEntity event = events.getFirst();

        assertThat(event.eventType())
                .isEqualTo(SecurityEventType.LOGIN_FAILED);
        assertThat(event.userId()).isNull();
        assertThat(event.usernameNormalized())
                .isEqualTo("unknown-user");
        assertThat(event.detail())
                .isEqualTo("Authentication failed.")
                .doesNotContain(
                        "incorrect-password",
                        TEST_PASSWORD);
    }

    @Test
    void recordsSuccessfulLoginForResponsibleUser()
            throws Exception {

        securityEventRepository.deleteAll();

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "  RESPONSIBLE  ")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        List<SecurityEventEntity> events =
                securityEventRepository
                        .findAllByOrderByOccurredAtAsc();

        assertThat(events)
                .extracting(SecurityEventEntity::eventType)
                .containsExactly(
                        SecurityEventType.LOGIN_SUCCEEDED);

        SecurityEventEntity event = events.getFirst();

        assertThat(event.userId())
                .isEqualTo(IdentityUserEntity.RESPONSIBLE_USER_ID);
        assertThat(event.usernameNormalized())
                .isEqualTo("responsible");
        assertThat(event.detail())
                .isEqualTo("Authentication succeeded.")
                .doesNotContain(TEST_PASSWORD);
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
