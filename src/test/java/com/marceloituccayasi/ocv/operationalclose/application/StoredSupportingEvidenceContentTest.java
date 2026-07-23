package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

class StoredSupportingEvidenceContentTest {

    private static final String SHA_256 =
            "abcdef0123456789abcdef0123456789"
                    + "abcdef0123456789abcdef0123456789";

    private static final SupportingEvidenceStorageReference REFERENCE =
            SupportingEvidenceStorageReference.create(
                    new SupportingEvidenceId(
                            UUID.fromString(
                                    "69404296-873f-480d-8774-588575100001")),
                    SHA_256,
                    "png");

    @Test
    void protectsStoredContentFromExternalMutation() {
        byte[] original =
                new byte[] {
                        1,
                        2,
                        3
                };

        StoredSupportingEvidenceContent storedContent =
                new StoredSupportingEvidenceContent(
                        REFERENCE,
                        original);

        original[0] =
                9;

        byte[] returned =
                storedContent.content();

        returned[1] =
                8;

        assertThat(storedContent.content())
                .containsExactly(
                        1,
                        2,
                        3);

        assertThat(storedContent.size())
                .isEqualTo(3);

        assertThat(storedContent.mediaType())
                .isEqualTo(
                        "image/png");

        assertThat(storedContent.extension())
                .isEqualTo(
                        "png");
    }

    @Test
    void rejectsEmptyContent() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            new StoredSupportingEvidenceContent(
                                    REFERENCE,
                                    new byte[0]))
                .withMessage(
                        "stored evidence content must not be empty");
    }

}