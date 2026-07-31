package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LoginRateLimitConfigurationTest {

    private static final String VALID_HASH =
            "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            IdentityAccessPropertiesConfiguration.class)
                    .withPropertyValues(
                            "ocv.auth.username=responsible",
                            "ocv.auth.password-hash=" + VALID_HASH);

    @Test
    void bindsValidExternalLoginRateLimitConfiguration() {
        contextRunner
                .withPropertyValues(
                        "ocv.login.max-failures=4",
                        "ocv.login.window-seconds=60",
                        "ocv.login.block-seconds=120")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(
                                    LoginRateLimitProperties.class);

                    LoginRateLimitProperties properties =
                            context.getBean(
                                    LoginRateLimitProperties.class);

                    assertThat(properties.maxFailures())
                            .isEqualTo(4);
                    assertThat(properties.windowSeconds())
                            .isEqualTo(60);
                    assertThat(properties.blockSeconds())
                            .isEqualTo(120);
                });
    }

    @Test
    void failsStartupWhenMaximumFailuresIsBelowMinimum() {
        contextRunner
                .withPropertyValues(
                        "ocv.login.max-failures=0",
                        "ocv.login.window-seconds=300",
                        "ocv.login.block-seconds=300")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(failureMessages(
                            context.getStartupFailure()))
                            .contains(
                                    "OCV_LOGIN_MAX_FAILURES must be at least 1");
                });
    }

    @Test
    void failsStartupWhenDurationsAreBelowMinimum() {
        contextRunner
                .withPropertyValues(
                        "ocv.login.max-failures=10",
                        "ocv.login.window-seconds=0",
                        "ocv.login.block-seconds=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(failureMessages(
                            context.getStartupFailure()))
                            .contains(
                                    "OCV_LOGIN_WINDOW_SECONDS must be at least 1",
                                    "OCV_LOGIN_BLOCK_SECONDS must be at least 1");
                });
    }

    private static String failureMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;

        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage())
                        .append(System.lineSeparator());
            }

            current = current.getCause();
        }

        return messages.toString();
    }

}