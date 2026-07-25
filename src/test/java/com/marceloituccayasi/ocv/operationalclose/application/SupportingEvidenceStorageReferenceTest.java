package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

class SupportingEvidenceStorageReferenceTest {

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "13012022-a429-48fe-a920-49ae5e100001");

    private static final SupportingEvidenceId EVIDENCE_ID =
            new SupportingEvidenceId(
                    EVIDENCE_UUID);

    private static final String SHA_256 =
            "0123456789abcdef0123456789abcdef"
                    + "0123456789abcdef0123456789abcdef";

    @Test
    void createsExactStoredReferenceAndDerivesSafeMediaType() {
        SupportingEvidenceStorageReference reference =
                SupportingEvidenceStorageReference.create(
                        EVIDENCE_ID,
                        SHA_256,
                        "pdf");

        assertThat(reference.value())
                .isEqualTo(
                        "stored:evidence/"
                                + EVIDENCE_UUID
                                + "/"
                                + SHA_256
                                + ".pdf");

        assertThat(reference.mediaType())
                .isEqualTo(
                        "application/pdf");
    }

    @Test
    void parsesGeneratedStoredReference() {
        String value =
                "stored:evidence/"
                        + EVIDENCE_UUID
                        + "/"
                        + SHA_256
                        + ".jpeg";

        SupportingEvidenceStorageReference reference =
                SupportingEvidenceStorageReference.parse(
                        value);

        assertThat(reference.evidenceId())
                .isEqualTo(
                        EVIDENCE_ID);

        assertThat(reference.sha256())
                .isEqualTo(
                        SHA_256);

        assertThat(reference.extension())
                .isEqualTo(
                        "jpeg");

        assertThat(reference.mediaType())
                .isEqualTo(
                        "image/jpeg");

        assertThat(reference.value())
                .isEqualTo(
                        value);
    }

    @Test
    void rejectsTextualBusinessReference() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            SupportingEvidenceStorageReference.parse(
                                    "reference:AUTH-2026-0001"))
                .withMessage(
                        "stored evidence content reference is invalid");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            SupportingEvidenceStorageReference.parse(
                                    "stored:evidence/"
                                            + EVIDENCE_UUID
                                            + "/../"
                                            + SHA_256
                                            + ".pdf"))
                .withMessage(
                        "stored evidence content reference is invalid");
    }

    @Test
    void rejectsUppercaseOrMalformedDigest() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            SupportingEvidenceStorageReference.create(
                                    EVIDENCE_ID,
                                    SHA_256.toUpperCase(),
                                    "pdf"))
                .withMessage(
                        "sha256 must contain exactly 64 lowercase hexadecimal characters");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            SupportingEvidenceStorageReference.create(
                                    EVIDENCE_ID,
                                    SHA_256,
                                    "svg"))
                .withMessage(
                        "stored evidence extension is not allowed");
    }

    @Test
    void rejectsSurroundingWhitespaceWithoutNormalizingIt() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            SupportingEvidenceStorageReference.parse(
                                    " stored:evidence/"
                                            + EVIDENCE_UUID
                                            + "/"
                                            + SHA_256
                                            + ".png"))
                .withMessage(
                        "contentReference must not contain surrounding whitespace");
    }

}