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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
class LoginRateLimitIntegrationTest {

    private static final String TEST_PASSWORD =
            "rate-limit-test-password";

    private static final String BLOCKED_ADDRESS =
            "198.51.100.20";

    private static final String OTHER_ADDRESS =
            "198.51.100.21";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityUserJpaRepository
            identityUserRepository;

    @Autowired
    private SecurityEventJpaRepository
            securityEventRepository;

    @Autowired
    private ResponsibleUserProvisioner provisioner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginRateLimiter rateLimiter;

    @Autowired
    private SessionRegistry sessionRegistry;

    @BeforeEach
    void prepareKnownCredential() {
        clearSessions();
        rateLimiter.clearAll();

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
        securityEventRepository.deleteAll();
    }

    @AfterEach
    void cleanRateLimiterAndSessions() {
        clearSessions();
        rateLimiter.clearAll();
    }

    @Test
    void blocksCredentialVerificationAfterTenFailures()
            throws Exception {

        for (int attempt = 0;
                attempt < 10;
                attempt++) {

            failedLogin(
                    BLOCKED_ADDRESS,
                    attempt % 2 == 0
                            ? " RESPONSIBLE "
                            : "responsible");
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(remoteAddress(
                                BLOCKED_ADDRESS))
                        .param(
                                "username",
                                "RESPONSIBLE")
                        .param(
                                "password",
                                TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?error"));

        List<SecurityEventType> eventTypes =
                securityEventRepository
                        .findAllByOrderByOccurredAtAsc()
                        .stream()
                        .map(
                                SecurityEventEntity::eventType)
                        .toList();

        assertThat(eventTypes)
                .filteredOn(type ->
                        type == SecurityEventType.LOGIN_FAILED)
                .hasSize(10);

        assertThat(eventTypes)
                .filteredOn(type ->
                        type == SecurityEventType.LOGIN_RATE_LIMITED)
                .hasSize(2);

        assertThat(eventTypes)
                .doesNotContain(
                        SecurityEventType.LOGIN_SUCCEEDED);
    }

    @Test
    void keepsDifferentSourceAddressIndependent()
            throws Exception {

        for (int attempt = 0;
                attempt < 10;
                attempt++) {

            failedLogin(
                    BLOCKED_ADDRESS,
                    "responsible");
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(remoteAddress(
                                OTHER_ADDRESS))
                        .param(
                                "username",
                                "responsible")
                        .param(
                                "password",
                                TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertThat(eventTypes())
                .contains(
                        SecurityEventType.LOGIN_SUCCEEDED);
    }

    @Test
    void successfulLoginClearsPreviousFailures()
            throws Exception {

        for (int attempt = 0;
                attempt < 9;
                attempt++) {

            failedLogin(
                    BLOCKED_ADDRESS,
                    "responsible");
        }

        successfulLogin(BLOCKED_ADDRESS);

        securityEventRepository.deleteAll();

        for (int attempt = 0;
                attempt < 9;
                attempt++) {

            failedLogin(
                    BLOCKED_ADDRESS,
                    "responsible");
        }

        assertThat(eventTypes())
                .containsOnly(
                        SecurityEventType.LOGIN_FAILED);
    }

    @Test
    void csrfFailureDoesNotConsumeRateLimitAttempt()
            throws Exception {

        for (int attempt = 0;
                attempt < 9;
                attempt++) {

            failedLogin(
                    BLOCKED_ADDRESS,
                    "responsible");
        }

        mockMvc.perform(post("/login")
                        .with(remoteAddress(
                                BLOCKED_ADDRESS))
                        .param(
                                "username",
                                "responsible")
                        .param(
                                "password",
                                "incorrect-password"))
                .andExpect(status().isForbidden());

        successfulLogin(BLOCKED_ADDRESS);

        assertThat(eventTypes())
                .contains(
                        SecurityEventType.LOGIN_SUCCEEDED)
                .doesNotContain(
                        SecurityEventType.LOGIN_RATE_LIMITED);
    }

    private void failedLogin(
            String sourceAddress,
            String username)
            throws Exception {

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(remoteAddress(
                                sourceAddress))
                        .param(
                                "username",
                                username)
                        .param(
                                "password",
                                "incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?error"));
    }

    private void successfulLogin(
            String sourceAddress)
            throws Exception {

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(remoteAddress(
                                sourceAddress))
                        .param(
                                "username",
                                "responsible")
                        .param(
                                "password",
                                TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    private List<SecurityEventType> eventTypes() {
        return securityEventRepository
                .findAllByOrderByOccurredAtAsc()
                .stream()
                .map(SecurityEventEntity::eventType)
                .toList();
    }

    private static RequestPostProcessor remoteAddress(
            String address) {

        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private void clearSessions() {
        sessionRegistry.getAllPrincipals()
                .forEach(principal ->
                        sessionRegistry
                                .getAllSessions(
                                        principal,
                                        true)
                                .forEach(session ->
                                        sessionRegistry
                                                .removeSessionInformation(
                                                        session.getSessionId())));
    }

}
