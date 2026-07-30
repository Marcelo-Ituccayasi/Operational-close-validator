package com.marceloituccayasi.ocv.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class OperationalRuntimeConfigurationValidatorTest {

    private static final String STORAGE_PATH_PROPERTY =
            "ocv.evidence.storage.path";

    private static final String COOKIE_SECURE_PROPERTY =
            "server.servlet.session.cookie.secure";

    @TempDir
    private Path storageRoot;

    @Test
    void validatesStorageThroughWriteReadAndDelete()
            throws IOException {

        MockEnvironment environment =
                localEnvironment();

        assertThatCode(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .doesNotThrowAnyException();

        try (var storedFiles =
                Files.list(
                        storageRoot)) {

            assertThat(
                    storedFiles)
                    .isEmpty();
        }
    }

    @Test
    void rejectsMissingStorageConfiguration() {
        MockEnvironment environment =
                new MockEnvironment();

        assertThatThrownBy(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "Supporting Evidence storage "
                                + "configuration is invalid.");
    }

    @Test
    void rejectsUnavailableStorageWithoutExposingPath() {
        Path missingPath =
                storageRoot.resolve(
                        "missing-volume");

        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty(
                                STORAGE_PATH_PROPERTY,
                                missingPath.toString());

        assertThatThrownBy(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "Supporting Evidence storage "
                                + "configuration is invalid.")
                .hasMessageNotContaining(
                        missingPath.toString());
    }

    @Test
    void acceptsApprovedPublicRuntimeConfiguration() {
        MockEnvironment environment =
                validPublicEnvironment();

        assertThatCode(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPublicRuntimeWithoutApprovedEnvironment() {
        MockEnvironment environment =
                publicEnvironment();

        environment.withProperty(
                COOKIE_SECURE_PROPERTY,
                "true");

        environment.withProperty(
                "OCV_TRUSTED_PROXY_CIDRS",
                "10.0.0.0/8");

        assertThatThrownBy(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "Public runtime configuration is invalid: "
                                + "OCV_ENVIRONMENT must be staging "
                                + "or production.");
    }

    @Test
    void rejectsInsecureCookieInPublicRuntime() {
        MockEnvironment environment =
                validPublicEnvironment();

        environment.withProperty(
                COOKIE_SECURE_PROPERTY,
                "false");

        assertThatThrownBy(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "Public runtime configuration is invalid: "
                                + "Secure session cookie is required.");
    }

    @Test
    void rejectsPublicRuntimeWithoutTrustedProxy() {
        MockEnvironment environment =
                publicEnvironment();

        environment.withProperty(
                "OCV_ENVIRONMENT",
                "production");

        environment.withProperty(
                COOKIE_SECURE_PROPERTY,
                "true");

        assertThatThrownBy(() ->
                validatorFor(
                        environment)
                        .afterPropertiesSet())
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "Public runtime configuration is invalid: "
                                + "OCV_TRUSTED_PROXY_CIDRS "
                                + "must not be blank.");
    }

    private MockEnvironment localEnvironment() {
        return new MockEnvironment()
                .withProperty(
                        STORAGE_PATH_PROPERTY,
                        storageRoot.toString());
    }

    private MockEnvironment publicEnvironment() {
        MockEnvironment environment =
                localEnvironment();

        environment.setActiveProfiles(
                "public");

        return environment;
    }

    private MockEnvironment validPublicEnvironment() {
        MockEnvironment environment =
                publicEnvironment();

        environment.withProperty(
                "OCV_ENVIRONMENT",
                "staging");

        environment.withProperty(
                COOKIE_SECURE_PROPERTY,
                "true");

        environment.withProperty(
                "OCV_TRUSTED_PROXY_CIDRS",
                "10.0.0.0/8");

        return environment;
    }

    private static OperationalRuntimeConfigurationValidator
            validatorFor(
                    MockEnvironment environment) {

        return new OperationalRuntimeConfigurationValidator(
                environment);
    }

}