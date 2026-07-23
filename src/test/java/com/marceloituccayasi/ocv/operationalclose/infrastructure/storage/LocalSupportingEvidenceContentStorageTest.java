package com.marceloituccayasi.ocv.operationalclose.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.marceloituccayasi.ocv.operationalclose.application.StoredSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.SupportingEvidenceStorageReference;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

class LocalSupportingEvidenceContentStorageTest {

    private static final long MAXIMUM_CONTENT_BYTES =
            10L * 1024L * 1024L;

    private static final SupportingEvidenceId EVIDENCE_ID =
            new SupportingEvidenceId(
                    UUID.fromString(
                            "905c16da-0e18-4f00-9289-042276100001"));

    @TempDir
    private Path storageRoot;

    @Test
    void storesFindsVerifiesAndDeletesContent() {
        byte[] content =
                new byte[] {
                        1,
                        2,
                        3,
                        4
                };

        SupportingEvidenceStorageReference reference =
                referenceFor(
                        content,
                        "pdf");

        LocalSupportingEvidenceContentStorage storage =
                storage();

        storage.store(
                reference,
                content);

        Optional<StoredSupportingEvidenceContent> found =
                storage.find(
                        reference);

        assertThat(found)
                .isPresent();

        assertThat(
                found.orElseThrow()
                        .content())
                .containsExactly(
                        content);

        assertThat(
                found.orElseThrow()
                        .mediaType())
                .isEqualTo(
                        "application/pdf");

        Path expectedPath =
                storageRoot
                        .resolve(
                                "evidence")
                        .resolve(
                                EVIDENCE_ID.value()
                                        .toString())
                        .resolve(
                                reference.sha256()
                                        + ".pdf");

        assertThat(expectedPath)
                .isRegularFile();

        storage.delete(
                reference);

        assertThat(
                storage.find(
                        reference))
                .isEmpty();
    }

    @Test
    void returnsEmptyWhenManagedContentDoesNotExist() {
        byte[] content =
                new byte[] {
                        5,
                        6,
                        7
                };

        LocalSupportingEvidenceContentStorage storage =
                storage();

        assertThat(
                storage.find(
                        referenceFor(
                                content,
                                "png")))
                .isEmpty();
    }

    @Test
    void rejectsContentWhoseDigestDoesNotMatchReference() {
        byte[] expectedContent =
                new byte[] {
                        1,
                        2,
                        3
                };

        byte[] differentContent =
                new byte[] {
                        4,
                        5,
                        6
                };

        LocalSupportingEvidenceContentStorage storage =
                storage();

        SupportingEvidenceStorageReference reference =
                referenceFor(
                        expectedContent,
                        "jpg");

        assertThatIllegalStateException()
                .isThrownBy(
                        () ->
                            storage.store(
                                    reference,
                                    differentContent))
                .withMessage(
                        "stored supporting evidence content integrity verification failed");
    }

    @Test
    void refusesContentModifiedAfterStorage() throws IOException {
        byte[] content =
                new byte[] {
                        10,
                        20,
                        30,
                        40
                };

        SupportingEvidenceStorageReference reference =
                referenceFor(
                        content,
                        "jpeg");

        LocalSupportingEvidenceContentStorage storage =
                storage();

        storage.store(
                reference,
                content);

        Path storedFile =
                storageRoot
                        .resolve(
                                "evidence")
                        .resolve(
                                EVIDENCE_ID.value()
                                        .toString())
                        .resolve(
                                reference.sha256()
                                        + ".jpeg");

        Files.write(
                storedFile,
                new byte[] {
                        11,
                        21,
                        31,
                        41
                });

        assertThatIllegalStateException()
                .isThrownBy(
                        () ->
                            storage.find(
                                    reference))
                .withMessage(
                        "stored supporting evidence content integrity verification failed");
    }

    @Test
    void rejectsOversizedContentBeforeCreatingDirectories() {
        LocalSupportingEvidenceContentStorage storage =
                new LocalSupportingEvidenceContentStorage(
                        storageRoot,
                        2L);

        byte[] content =
                new byte[] {
                        1,
                        2,
                        3
                };

        SupportingEvidenceStorageReference reference =
                referenceFor(
                        content,
                        "png");

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            storage.store(
                                    reference,
                                    content))
                .withMessage(
                        "stored supporting evidence content exceeds configured maximum");

        assertThat(
                storageRoot.resolve(
                        "evidence"))
                .doesNotExist();
    }

    @Test
    void rejectsRelativeStorageRoot() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            new LocalSupportingEvidenceContentStorage(
                                    Path.of(
                                            "relative-storage"),
                                    MAXIMUM_CONTENT_BYTES))
                .withMessage(
                        "supporting evidence storage root must be absolute");
    }

    @Test
    void rejectsMaximumAboveApprovedLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                            new LocalSupportingEvidenceContentStorage(
                                    storageRoot,
                                    MAXIMUM_CONTENT_BYTES + 1L))
                .withMessage(
                        "supporting evidence maximum content size must be between 1 and 10485760 bytes");
    }

    @Test
    void rejectsSymbolicManagedDirectoryWhenSupported()
            throws IOException {

        Path externalDirectory =
                Files.createTempDirectory(
                        "ocv-external-evidence-");

        Path evidenceDirectory =
                storageRoot.resolve(
                        "evidence");

        boolean symbolicLinkCreated =
                false;

        try {
            try {
                Files.createSymbolicLink(
                        evidenceDirectory,
                        externalDirectory);

                symbolicLinkCreated =
                        true;
            }
            catch (UnsupportedOperationException
                    | SecurityException
                    | IOException exception) {

                assumeTrue(
                        false,
                        "symbolic links are not available in this environment");
            }

            byte[] content =
                    new byte[] {
                            8,
                            9,
                            10
                    };

            LocalSupportingEvidenceContentStorage storage =
                    storage();

            SupportingEvidenceStorageReference reference =
                    referenceFor(
                            content,
                            "pdf");

            assertThatIllegalStateException()
                    .isThrownBy(
                            () ->
                                storage.store(
                                        reference,
                                        content))
                    .withMessage(
                            "supporting evidence storage path is unsafe");

            assertThat(
                    externalDirectory)
                    .isEmptyDirectory();
        }
        finally {
            if (symbolicLinkCreated) {
                Files.deleteIfExists(
                        evidenceDirectory);
            }

            Files.deleteIfExists(
                    externalDirectory);
        }
    }

    private LocalSupportingEvidenceContentStorage storage() {
        return new LocalSupportingEvidenceContentStorage(
                storageRoot,
                MAXIMUM_CONTENT_BYTES);
    }

    private static SupportingEvidenceStorageReference referenceFor(
            byte[] content,
            String extension) {

        return SupportingEvidenceStorageReference.create(
                EVIDENCE_ID,
                sha256Hex(
                        content),
                extension);
    }

    private static String sha256Hex(
            byte[] content) {

        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance(
                                    "SHA-256")
                                    .digest(
                                            content));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    exception);
        }
    }

}