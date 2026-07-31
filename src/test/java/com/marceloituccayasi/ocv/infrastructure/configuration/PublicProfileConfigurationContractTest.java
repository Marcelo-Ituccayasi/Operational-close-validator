package com.marceloituccayasi.ocv.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class PublicProfileConfigurationContractTest {

    @Test
    void configuresTrustedForwardedHeadersForTlsProxy()
            throws IOException {

        Properties properties =
                loadPublicProfileProperties();

        assertThat(properties.getProperty(
                "server.forward-headers-strategy"))
                .isEqualTo(
                        "native");

        assertThat(properties.getProperty(
                "server.tomcat.redirect-context-root"))
                .isEqualTo(
                        "false");

        assertThat(properties.getProperty(
                "server.tomcat.remoteip.internal-proxies"))
                .isEqualTo(
                        "${OCV_TRUSTED_PROXY_CIDRS}");

        assertThat(properties.getProperty(
                "server.tomcat.remoteip.remote-ip-header"))
                .isEqualTo(
                        "X-Forwarded-For");

        assertThat(properties.getProperty(
                "server.tomcat.remoteip.protocol-header"))
                .isEqualTo(
                        "X-Forwarded-Proto");

        assertThat(properties.getProperty(
                "server.tomcat.remoteip.host-header"))
                .isEqualTo(
                        "X-Forwarded-Host");

        assertThat(properties.getProperty(
                "server.tomcat.remoteip.port-header"))
                .isEqualTo(
                        "X-Forwarded-Port");
    }

    private static Properties loadPublicProfileProperties()
            throws IOException {

        Properties properties =
                new Properties();

        try (InputStream input =
                PublicProfileConfigurationContractTest.class
                        .getClassLoader()
                        .getResourceAsStream(
                                "application-public.properties")) {

            assertThat(input)
                    .as("application-public.properties must exist")
                    .isNotNull();

            properties.load(
                    input);
        }

        return properties;
    }

}