package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthenticationConfigurationFailFastTest {

    private static final String VALID_HASH =
            "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            IdentityAccessPropertiesConfiguration.class);

    @Test
    void startsWithValidExternalAuthenticationConfiguration() {
        contextRunner
                .withPropertyValues(
                        "ocv.auth.username=Responsible",
                        "ocv.auth.password-hash=" + VALID_HASH)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(AuthenticationProperties.class);

                    AuthenticationProperties properties =
                            context.getBean(
                                    AuthenticationProperties.class);

                    assertThat(properties.username())
                            .isEqualTo("responsible");
                    assertThat(properties.passwordHash())
                            .isEqualTo(VALID_HASH);
                });
    }

    @Test
    void failsStartupWhenAuthenticationConfigurationIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(failureMessages(
                    context.getStartupFailure()))
                    .contains(
                            "OCV_AUTH_USERNAME must not be blank",
                            "OCV_AUTH_PASSWORD_HASH must not be blank");
        });
    }

    @Test
    void failsStartupWhenPasswordHashIsNotDelegatingBcrypt() {
        contextRunner
                .withPropertyValues(
                        "ocv.auth.username=responsible",
                        "ocv.auth.password-hash=plain-text-password")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(failureMessages(
                            context.getStartupFailure()))
                            .contains(
                                    "OCV_AUTH_PASSWORD_HASH must use "
                                            + "{bcrypt}<encoded-password>");
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
