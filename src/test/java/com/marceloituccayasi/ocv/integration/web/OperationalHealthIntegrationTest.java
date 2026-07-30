package com.marceloituccayasi.ocv.integration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OperationalHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${management.endpoint.health.group.liveness.include}")
    private String livenessContributors;

    @Value("${management.endpoint.health.group.readiness.include}")
    private String readinessContributors;

    @Value("${management.endpoint.health.group.startup.include}")
    private String startupContributors;

    @Test
    void exposesSanitizedOperationalProbes()
            throws Exception {

        assertProbeIsUp(
                "/actuator/health/startup");

        assertProbeIsUp(
                "/actuator/health/liveness");

        assertProbeIsUp(
                "/actuator/health/readiness");
    }

    @Test
    void keepsGeneralHealthEndpointAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/actuator/health"))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/login"));
    }

    @Test
    void keepsLivenessIndependentOfExternalSystems() {
        assertThat(
                livenessContributors)
                .isEqualTo(
                        "livenessState");

        assertThat(
                readinessContributors.split(","))
                .containsExactly(
                        "readinessState",
                        "db",
                        "evidenceStorage");

        assertThat(
                startupContributors.split(","))
                .containsExactly(
                        "readinessState",
                        "db",
                        "evidenceStorage");
    }

    private void assertProbeIsUp(
            String endpoint)
            throws Exception {

        mockMvc.perform(
                        get(
                                endpoint))
                .andExpect(
                        status().isOk())
                .andExpect(
                        jsonPath(
                                "$.status")
                                .value(
                                        "UP"))
                .andExpect(
                        jsonPath(
                                "$.components")
                                .doesNotExist());
    }

}