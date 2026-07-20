package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.TrustedClientAddressResolver;

class TrustedProxyConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            TestConfiguration.class);

    @Test
    void startsWithValidTrustedProxyCidrs() {
        contextRunner
                .withPropertyValues(
                        "ocv.trusted.proxy.cidrs="
                                + "10.0.0.0/8,"
                                + "192.168.0.0/16")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(
                                    TrustedClientAddressResolver.class);
                });
    }

    @Test
    void failsStartupForInvalidTrustedProxyCidr() {
        contextRunner
                .withPropertyValues(
                        "ocv.trusted.proxy.cidrs="
                                + "10.0.0.0/99")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(failureMessages(
                            context.getStartupFailure()))
                            .contains(
                                    "Invalid trusted proxy CIDR: "
                                            + "10.0.0.0/99");
                });
    }

    private static String failureMessages(
            Throwable failure) {

        StringBuilder messages =
                new StringBuilder();

        Throwable current = failure;

        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(
                                current.getMessage())
                        .append(
                                System.lineSeparator());
            }

            current = current.getCause();
        }

        return messages.toString();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(
            TrustedProxyProperties.class)
    static class TestConfiguration {

        @Bean
        TrustedClientAddressResolver
                trustedClientAddressResolver(
                        TrustedProxyProperties properties) {

            return new TrustedClientAddressResolver(
                    properties);
        }

    }

}
