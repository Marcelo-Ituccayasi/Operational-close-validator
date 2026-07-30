package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingIntegrationTest {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    StructuredLoggingIntegrationTest.class);

    private static final String VERIFICATION_MESSAGE =
            "Structured logging verification";

    @AfterEach
    void clearMappedDiagnosticContext() {
        MDC.clear();
    }

    @Test
    void emitsApprovedEcsFieldsWithCorrelationId(
            CapturedOutput output) {

        String correlationId =
                UUID.randomUUID()
                        .toString();

        MDC.put(
                CorrelationIdFilter.MDC_KEY,
                correlationId);

        try {
            LOGGER.info(
                    VERIFICATION_MESSAGE);
        }
        finally {
            MDC.remove(
                    CorrelationIdFilter.MDC_KEY);
        }

        String structuredLine =
                output.getAll()
                        .lines()
                        .filter(line ->
                                line.contains(
                                        "\"message\":\""
                                                + VERIFICATION_MESSAGE
                                                + "\""))
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Structured verification log "
                                                + "was not captured."));

        assertThat(
                structuredLine)
                .startsWith(
                        "{")
                .contains(
                        "\"@timestamp\":")
                .contains(
                        "\"level\":\"INFO\"")
                .contains(
                        "\"logger\":\""
                                + StructuredLoggingIntegrationTest.class
                                        .getName()
                                + "\"")
                .contains(
                        "\"environment\":\"test\"")
                .contains(
                        "\"correlationId\":\""
                                + correlationId
                                + "\"")
                .contains(
                        "\"message\":\""
                                + VERIFICATION_MESSAGE
                                + "\"");
    }

}