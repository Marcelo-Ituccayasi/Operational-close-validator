package com.marceloituccayasi.ocv.operationalclose.infrastructure.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration.SupportingEvidenceStorageProperties;

class SupportingEvidenceStorageHealthIndicatorTest {

    @TempDir
    private Path storageRoot;

    @Test
    void reportsUpAfterWriteReadAndDeleteProbe()
            throws IOException {

        SupportingEvidenceStorageHealthIndicator indicator =
                indicatorFor(
                        storageRoot);

        Health health =
                indicator.health();

        assertThat(
                health.getStatus())
                .isEqualTo(
                        Status.UP);

        assertThat(
                health.getDetails())
                .isEmpty();

        try (var storedFiles =
                Files.list(
                        storageRoot)) {

            assertThat(
                    storedFiles)
                    .isEmpty();
        }
    }

    @Test
    void reportsDownWhenStorageRootDoesNotExist() {
        Path missingRoot =
                storageRoot.resolve(
                        "missing");

        Health health =
                indicatorFor(
                        missingRoot)
                        .health();

        assertThat(
                health.getStatus())
                .isEqualTo(
                        Status.DOWN);

        assertThat(
                health.getDetails())
                .isEmpty();
    }

    private static SupportingEvidenceStorageHealthIndicator
            indicatorFor(
                    Path storageRoot) {

        SupportingEvidenceStorageProperties properties =
                new SupportingEvidenceStorageProperties(
                        storageRoot.toString(),
                        1024L);

        return new SupportingEvidenceStorageHealthIndicator(
                properties);
    }

}