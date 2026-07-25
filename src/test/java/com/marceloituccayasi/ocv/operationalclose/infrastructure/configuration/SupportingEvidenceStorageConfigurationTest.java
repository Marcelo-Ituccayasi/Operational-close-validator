package com.marceloituccayasi.ocv.operationalclose.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.storage.LocalSupportingEvidenceContentStorage;

class SupportingEvidenceStorageConfigurationTest {

    private static final long MAXIMUM_CONTENT_BYTES =
            10L * 1024L * 1024L;

    @TempDir
    private Path storageRoot;

    private final SupportingEvidenceStorageConfiguration configuration =
            new SupportingEvidenceStorageConfiguration();

    @Test
    void createsLocalStorageFromValidatedProperties() {
        SupportingEvidenceStorageProperties properties =
                new SupportingEvidenceStorageProperties(
                        storageRoot.toString(),
                        MAXIMUM_CONTENT_BYTES);

        SupportingEvidenceContentStorage storage =
                configuration.supportingEvidenceContentStorage(
                        properties);

        assertThat(storage)
                .isInstanceOf(
                        LocalSupportingEvidenceContentStorage.class);
    }

    @Test
    void rejectsRelativeConfiguredStoragePath() {
        SupportingEvidenceStorageProperties properties =
                new SupportingEvidenceStorageProperties(
                        "relative-evidence-storage",
                        MAXIMUM_CONTENT_BYTES);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            configuration.supportingEvidenceContentStorage(
                                    properties))
                .withMessage(
                        "supporting evidence storage root must be absolute");
    }

    @Test
    void rejectsMissingConfiguredStorageDirectory() {
        Path missingDirectory =
                storageRoot.resolve(
                        "missing");

        SupportingEvidenceStorageProperties properties =
                new SupportingEvidenceStorageProperties(
                        missingDirectory.toString(),
                        MAXIMUM_CONTENT_BYTES);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            configuration.supportingEvidenceContentStorage(
                                    properties))
                .withMessage(
                        "supporting evidence storage root must exist");
    }

    @Test
    void rejectsSurroundingWhitespaceWithoutChangingConfiguredPath() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            new SupportingEvidenceStorageProperties(
                                    " "
                                            + storageRoot
                                            + " ",
                                    MAXIMUM_CONTENT_BYTES))
                .withMessage(
                        "OCV_EVIDENCE_STORAGE_PATH must not contain surrounding whitespace");
    }

}