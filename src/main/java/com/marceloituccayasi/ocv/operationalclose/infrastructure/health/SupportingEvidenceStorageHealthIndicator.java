package com.marceloituccayasi.ocv.operationalclose.infrastructure.health;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration.SupportingEvidenceStorageProperties;

/**
 * Verifies that the Supporting Evidence volume remains usable.
 */
@Component("evidenceStorageHealthIndicator")
public final class SupportingEvidenceStorageHealthIndicator
        implements HealthIndicator {

    private static final byte[] PROBE_CONTENT =
            "ocv-readiness"
                    .getBytes(
                            StandardCharsets.US_ASCII);

    private final Path storageRoot;

    public SupportingEvidenceStorageHealthIndicator(
            SupportingEvidenceStorageProperties properties) {

        this.storageRoot =
                Path.of(
                        properties.path())
                        .normalize();
    }

    @Override
    public Health health() {
        Path probeFile =
                null;

        try {
            if (!isUsableStorageRoot()) {
                return Health.down()
                        .build();
            }

            probeFile =
                    Files.createTempFile(
                            storageRoot,
                            ".ocv-health-",
                            ".tmp");

            Files.write(
                    probeFile,
                    PROBE_CONTENT,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            byte[] restoredContent =
                    Files.readAllBytes(
                            probeFile);

            if (!Arrays.equals(
                    PROBE_CONTENT,
                    restoredContent)) {

                return Health.down()
                        .build();
            }

            Files.delete(
                    probeFile);

            probeFile =
                    null;

            return Health.up()
                    .build();
        }
        catch (IOException | SecurityException exception) {
            return Health.down()
                    .build();
        }
        finally {
            deleteProbeQuietly(
                    probeFile);
        }
    }

    private boolean isUsableStorageRoot() {
        return Files.exists(
                        storageRoot,
                        NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(
                        storageRoot)
                && Files.isDirectory(
                        storageRoot,
                        NOFOLLOW_LINKS)
                && Files.isReadable(
                        storageRoot)
                && Files.isWritable(
                        storageRoot);
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
            // The health result is already DOWN when cleanup is relevant.
        }
    }

}