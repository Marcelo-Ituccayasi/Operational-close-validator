package com.marceloituccayasi.ocv.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class DeploymentConfigurationContractTest {

    @Test
    void exposesApprovedDeploymentEnvironmentVariables()
            throws IOException {

        Properties properties = loadApplicationProperties();

        assertThat(properties.getProperty(
                "ocv.business.time-zone"))
                .isEqualTo(
                        "${OCV_BUSINESS_TIME_ZONE:UTC}");

        assertThat(properties.getProperty(
                "server.servlet.session.timeout"))
                .isEqualTo(
                        "${OCV_SESSION_EXPIRY_MINUTES:30}m");

        assertThat(properties.getProperty(
                "spring.servlet.multipart.max-file-size"))
                .isEqualTo(
                        "${OCV_EVIDENCE_MAX_FILE_SIZE_BYTES:10485760}B");

        assertThat(properties.getProperty(
                "spring.servlet.multipart.max-request-size"))
                .isEqualTo(
                        "${OCV_EVIDENCE_MAX_REQUEST_SIZE_BYTES:12582912}B");

        assertThat(properties.getProperty(
                "ocv.evidence.storage.maximum-content-bytes"))
                .isEqualTo(
                        "${OCV_EVIDENCE_MAX_FILE_SIZE_BYTES:10485760}");

        assertThat(properties.values())
                .allSatisfy(value ->
                        assertThat(value.toString())
                                .doesNotContain(
                                        "OCV_EVIDENCE_STORAGE_"
                                                + "MAXIMUM_CONTENT_BYTES"));
    }

    private static Properties loadApplicationProperties()
            throws IOException {

        Properties properties = new Properties();

        try (InputStream input =
                DeploymentConfigurationContractTest.class
                        .getClassLoader()
                        .getResourceAsStream(
                                "application.properties")) {

            assertThat(input)
                    .as("application.properties must exist")
                    .isNotNull();

            properties.load(input);
        }

        return properties;
    }

}