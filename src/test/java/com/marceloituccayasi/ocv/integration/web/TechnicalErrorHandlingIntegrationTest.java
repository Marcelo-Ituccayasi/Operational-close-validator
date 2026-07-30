package com.marceloituccayasi.ocv.integration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.CorrelationIdFilter;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationAlertJpaRepository;
import com.marceloituccayasi.ocv.presentation.TechnicalErrorHandler;

@ActiveProfiles("test")
@Import({
        TestcontainersConfiguration.class,
        TechnicalErrorHandlingIntegrationTest
                .TechnicalFailureController.class
})
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class TechnicalErrorHandlingIntegrationTest {

    private static final String SENSITIVE_MARKER =
            "SENSITIVE_TEST_MARKER "
                    + "{bcrypt}$2a$10$secret "
                    + "C:\\private\\evidence";

    private static final String FAILURE_ENDPOINT =
            "/test/technical-failure";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ValidationAlertJpaRepository alertRepository;

    @Value("${spring.web.error.include-exception}")
    private boolean includeException;

    @Value("${spring.web.error.include-message}")
    private String includeMessage;

    @Value("${spring.web.error.include-stacktrace}")
    private String includeStacktrace;

    @Value("${spring.web.error.include-binding-errors}")
    private String includeBindingErrors;

    @Value("${spring.web.error.include-path}")
    private String includePath;

    @Value("${spring.web.error.whitelabel.enabled}")
    private boolean whitelabelEnabled;

    @Value("${spring.mvc.log-request-details}")
    private boolean logRequestDetails;

    @Value("${spring.mvc.log-resolved-exception}")
    private boolean logResolvedException;

    @Test
    void configuresSanitizedFallbackErrorHandling() {
        assertThat(
                includeException)
                .isFalse();

        assertThat(
                includeMessage)
                .isEqualTo(
                        "never");

        assertThat(
                includeStacktrace)
                .isEqualTo(
                        "never");

        assertThat(
                includeBindingErrors)
                .isEqualTo(
                        "never");

        assertThat(
                includePath)
                .isEqualTo(
                        "never");

        assertThat(
                whitelabelEnabled)
                .isFalse();

        assertThat(
                logRequestDetails)
                .isFalse();

        assertThat(
                logResolvedException)
                .isFalse();
    }

    @Test
    void sanitizesTechnicalFailureWithoutCreatingBusinessAlert(
            CapturedOutput output)
            throws Exception {

        long alertCountBefore =
                alertRepository.count();

        MvcResult mvcResult =
                mockMvc.perform(
                                get(
                                        FAILURE_ENDPOINT)
                                        .with(
                                                user(
                                                        "responsible")))
                        .andExpect(
                                status().isInternalServerError())
                        .andExpect(
                                view().name(
                                        "errors/status"))
                        .andExpect(
                                model().attribute(
                                        "statusCode",
                                        500))
                        .andExpect(
                                model().attribute(
                                        "title",
                                        TechnicalErrorHandler.USER_TITLE))
                        .andExpect(
                                model().attribute(
                                        "message",
                                        TechnicalErrorHandler.USER_MESSAGE))
                        .andExpect(
                                header().exists(
                                        CorrelationIdFilter.HEADER_NAME))
                        .andReturn();

        String responseBody =
                mvcResult.getResponse()
                        .getContentAsString();

        String correlationId =
                mvcResult.getResponse()
                        .getHeader(
                                CorrelationIdFilter.HEADER_NAME);

        assertThat(
                responseBody)
                .contains(
                        TechnicalErrorHandler.USER_TITLE)
                .contains(
                        TechnicalErrorHandler.USER_MESSAGE)
                .doesNotContain(
                        SENSITIVE_MARKER,
                        "IllegalStateException",
                        "{bcrypt}",
                        "C:\\private\\evidence");

        assertThat(
                alertRepository.count())
                .isEqualTo(
                        alertCountBefore);

        String technicalLog =
                output.getAll()
                        .lines()
                        .filter(line ->
                                line.contains(
                                        "\"message\":\""
                                                + TechnicalErrorHandler.LOG_MESSAGE
                                                + "\""))
                        .reduce(
                                (first, second) ->
                                        second)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Technical error log "
                                                + "was not captured."));

        assertThat(
                technicalLog)
                .contains(
                        "TECHNICAL_ERROR")
                .contains(
                        "unexpected_failure")
                .contains(
                        "IllegalStateException")
                .contains(
                        correlationId)
                .doesNotContain(
                        SENSITIVE_MARKER,
                        "{bcrypt}",
                        "C:\\private\\evidence");
    }

    @Controller
    static final class TechnicalFailureController {

        @GetMapping(FAILURE_ENDPOINT)
        String failTechnically() {
            throw new IllegalStateException(
                    SENSITIVE_MARKER);
        }
    }

}