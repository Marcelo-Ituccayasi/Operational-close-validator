package com.marceloituccayasi.ocv.infrastructure.configuration;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Stops application startup when runtime configuration is incomplete or unsafe.
 */
@Component
public final class OperationalRuntimeConfigurationValidator
        implements InitializingBean {

    private static final String PUBLIC_PROFILE =
            "public";

    private static final String STORAGE_PATH_PROPERTY =
            "ocv.evidence.storage.path";

    private static final String COOKIE_SECURE_PROPERTY =
            "server.servlet.session.cookie.secure";

    private static final String OCV_ENVIRONMENT_VARIABLE =
            "OCV_ENVIRONMENT";

    private static final String OCV_ENVIRONMENT_PROPERTY =
            "ocv.environment";

    private static final String TRUSTED_PROXY_VARIABLE =
            "OCV_TRUSTED_PROXY_CIDRS";

    private static final String TRUSTED_PROXY_PROPERTY =
            "ocv.trusted.proxy.cidrs";

    private static final Set<String> PUBLIC_ENVIRONMENTS =
            Set.of(
                    "staging",
                    "production");

    private static final byte[] STORAGE_PROBE_CONTENT =
            "ocv-startup-validation"
                    .getBytes(
                            StandardCharsets.US_ASCII);

    private final Environment environment;

    public OperationalRuntimeConfigurationValidator(
            Environment environment) {

        this.environment =
                environment;
    }

    @Override
    public void afterPropertiesSet() {
        validateSupportingEvidenceStorage();

        if (environment.acceptsProfiles(
                Profiles.of(
                        PUBLIC_PROFILE))) {

            validatePublicRuntime();
        }
    }

    private void validatePublicRuntime() {
        String runtimeEnvironment =
                firstConfiguredValue(
                        OCV_ENVIRONMENT_VARIABLE,
                        OCV_ENVIRONMENT_PROPERTY);

        if (runtimeEnvironment == null
                || !PUBLIC_ENVIRONMENTS.contains(
                        runtimeEnvironment)) {

            throw new IllegalStateException(
                    "Public runtime configuration is invalid: "
                            + "OCV_ENVIRONMENT must be staging "
                            + "or production.");
        }

        Boolean secureCookie =
                environment.getProperty(
                        COOKIE_SECURE_PROPERTY,
                        Boolean.class);

        if (!Boolean.TRUE.equals(
                secureCookie)) {

            throw new IllegalStateException(
                    "Public runtime configuration is invalid: "
                            + "Secure session cookie is required.");
        }

        String trustedProxyCidrs =
                firstConfiguredValue(
                        TRUSTED_PROXY_VARIABLE,
                        TRUSTED_PROXY_PROPERTY);

        if (trustedProxyCidrs == null
                || trustedProxyCidrs.isBlank()) {

            throw new IllegalStateException(
                    "Public runtime configuration is invalid: "
                            + "OCV_TRUSTED_PROXY_CIDRS "
                            + "must not be blank.");
        }
    }

    private void validateSupportingEvidenceStorage() {
        String configuredPath =
                environment.getProperty(
                        STORAGE_PATH_PROPERTY);

        if (configuredPath == null
                || configuredPath.isBlank()
                || !configuredPath.equals(
                        configuredPath.trim())) {

            throw storageConfigurationFailure();
        }

        Path storageRoot;

        try {
            storageRoot =
                    Path.of(
                            configuredPath)
                            .normalize();
        }
        catch (InvalidPathException exception) {
            throw storageConfigurationFailure();
        }

        if (!storageRoot.isAbsolute()
                || !Files.exists(
                        storageRoot,
                        NOFOLLOW_LINKS)
                || Files.isSymbolicLink(
                        storageRoot)
                || !Files.isDirectory(
                        storageRoot,
                        NOFOLLOW_LINKS)
                || !Files.isReadable(
                        storageRoot)
                || !Files.isWritable(
                        storageRoot)) {

            throw storageConfigurationFailure();
        }

        verifyStorageInputOutput(
                storageRoot);
    }

    private static void verifyStorageInputOutput(
            Path storageRoot) {

        Path probeFile =
                null;

        try {
            probeFile =
                    Files.createTempFile(
                            storageRoot,
                            ".ocv-startup-",
                            ".tmp");

            Files.write(
                    probeFile,
                    STORAGE_PROBE_CONTENT,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            byte[] restoredContent =
                    Files.readAllBytes(
                            probeFile);

            if (!Arrays.equals(
                    STORAGE_PROBE_CONTENT,
                    restoredContent)) {

                throw storageConfigurationFailure();
            }

            Files.delete(
                    probeFile);

            probeFile =
                    null;
        }
        catch (IOException | SecurityException exception) {
            throw storageConfigurationFailure();
        }
        finally {
            deleteProbeQuietly(
                    probeFile);
        }
    }

    private String firstConfiguredValue(
            String environmentVariable,
            String canonicalProperty) {

        String value =
                environment.getProperty(
                        environmentVariable);

        if (value != null) {
            return value;
        }

        return environment.getProperty(
                canonicalProperty);
    }

    private static IllegalStateException
            storageConfigurationFailure() {

        return new IllegalStateException(
                "Supporting Evidence storage configuration "
                        + "is invalid.");
    }

    private static void deleteProbeQuietly(
            Path probeFile) {

        if (probeFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(
                    probeFile);
        }
        catch (IOException | SecurityException exception) {
            // Startup will already fail with a sanitized result.
        }
    }

}