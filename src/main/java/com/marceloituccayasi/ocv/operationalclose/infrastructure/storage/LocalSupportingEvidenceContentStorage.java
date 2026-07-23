package com.marceloituccayasi.ocv.operationalclose.infrastructure.storage;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.application.StoredSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.SupportingEvidenceStorageReference;
import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;

/**
 * Stores application-managed Supporting Evidence content below one configured
 * local directory.
 *
 * <p>The browser and Presentation layer never provide physical paths. Every
 * target path is derived exclusively from a validated
 * {@link SupportingEvidenceStorageReference}.
 */
public final class LocalSupportingEvidenceContentStorage
        implements SupportingEvidenceContentStorage {

    public static final long MAXIMUM_APPROVED_CONTENT_BYTES =
            10L * 1024L * 1024L;

    private static final String EVIDENCE_DIRECTORY =
            "evidence";

    private final Path storageRoot;

    private final long maximumContentBytes;

    public LocalSupportingEvidenceContentStorage(
            Path storageRoot,
            long maximumContentBytes) {

        Objects.requireNonNull(
                storageRoot,
                "storageRoot must not be null");

        if (!storageRoot.isAbsolute()) {
            throw new IllegalArgumentException(
                    "supporting evidence storage root must be absolute");
        }

        this.storageRoot =
                storageRoot.normalize();

        if (maximumContentBytes < 1L
                || maximumContentBytes
                        > MAXIMUM_APPROVED_CONTENT_BYTES) {

            throw new IllegalArgumentException(
                    "supporting evidence maximum content size must be between 1 and 10485760 bytes");
        }

        this.maximumContentBytes =
                maximumContentBytes;

        validateStorageRoot();
    }

    @Override
    public void store(
            SupportingEvidenceStorageReference reference,
            byte[] content) {

        Objects.requireNonNull(
                reference,
                "reference must not be null");

        Objects.requireNonNull(
                content,
                "content must not be null");

        byte[] contentCopy =
                content.clone();

        validateInputSize(
                contentCopy);

        verifyDigest(
                reference,
                contentCopy);

        validateStorageRoot();

        Path target =
                targetPath(
                        reference);

        Path evidenceDirectory =
                storageRoot.resolve(
                        EVIDENCE_DIRECTORY);

        Path authorizationDirectory =
                target.getParent();

        Path temporaryFile =
                null;

        try {
            ensureManagedDirectory(
                    evidenceDirectory);

            ensureManagedDirectory(
                    authorizationDirectory);

            if (Files.exists(
                    target,
                    NOFOLLOW_LINKS)) {

                if (Files.isSymbolicLink(
                        target)
                        || !Files.isRegularFile(
                                target,
                                NOFOLLOW_LINKS)) {

                    throw unsafeStoragePath();
                }

                throw new IllegalStateException(
                        "stored supporting evidence content already exists");
            }

            temporaryFile =
                    Files.createTempFile(
                            authorizationDirectory,
                            ".evidence-upload-",
                            ".tmp");

            Files.write(
                    temporaryFile,
                    contentCopy,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            moveWithoutReplacement(
                    temporaryFile,
                    target);

            temporaryFile =
                    null;
        }
        catch (IOException exception) {
            throw storageFailure(
                    exception);
        }
        finally {
            deleteTemporaryFileQuietly(
                    temporaryFile);
        }
    }

    @Override
    public Optional<StoredSupportingEvidenceContent> find(
            SupportingEvidenceStorageReference reference) {

        Objects.requireNonNull(
                reference,
                "reference must not be null");

        validateStorageRoot();

        Path target =
                targetPath(
                        reference);

        Path evidenceDirectory =
                storageRoot.resolve(
                        EVIDENCE_DIRECTORY);

        Path authorizationDirectory =
                target.getParent();

        try {
            if (!validateExistingManagedDirectory(
                    evidenceDirectory)) {

                return Optional.empty();
            }

            if (!validateExistingManagedDirectory(
                    authorizationDirectory)) {

                return Optional.empty();
            }

            if (!Files.exists(
                    target,
                    NOFOLLOW_LINKS)) {

                return Optional.empty();
            }

            if (Files.isSymbolicLink(
                    target)
                    || !Files.isRegularFile(
                            target,
                            NOFOLLOW_LINKS)) {

                throw unsafeStoragePath();
            }

            long fileSize =
                    Files.size(
                            target);

            if (fileSize < 1L
                    || fileSize > maximumContentBytes) {

                throw integrityFailure();
            }

            byte[] content =
                    Files.readAllBytes(
                            target);

            if (content.length != fileSize) {
                throw integrityFailure();
            }

            verifyDigest(
                    reference,
                    content);

            return Optional.of(
                    new StoredSupportingEvidenceContent(
                            reference,
                            content));
        }
        catch (IOException exception) {
            throw storageFailure(
                    exception);
        }
    }

    @Override
    public void delete(
            SupportingEvidenceStorageReference reference) {

        Objects.requireNonNull(
                reference,
                "reference must not be null");

        validateStorageRoot();

        Path target =
                targetPath(
                        reference);

        Path evidenceDirectory =
                storageRoot.resolve(
                        EVIDENCE_DIRECTORY);

        Path authorizationDirectory =
                target.getParent();

        try {
            if (!validateExistingManagedDirectory(
                    evidenceDirectory)) {

                return;
            }

            if (!validateExistingManagedDirectory(
                    authorizationDirectory)) {

                return;
            }

            if (!Files.exists(
                    target,
                    NOFOLLOW_LINKS)) {

                return;
            }

            if (Files.isSymbolicLink(
                    target)
                    || !Files.isRegularFile(
                            target,
                            NOFOLLOW_LINKS)) {

                throw unsafeStoragePath();
            }

            Files.delete(
                    target);
        }
        catch (IOException exception) {
            throw storageFailure(
                    exception);
        }
    }

    private void validateStorageRoot() {
        if (!Files.exists(
                storageRoot,
                NOFOLLOW_LINKS)) {

            throw new IllegalArgumentException(
                    "supporting evidence storage root must exist");
        }

        if (Files.isSymbolicLink(
                storageRoot)
                || !Files.isDirectory(
                        storageRoot,
                        NOFOLLOW_LINKS)) {

            throw new IllegalArgumentException(
                    "supporting evidence storage root must be a non-symbolic directory");
        }

        if (!Files.isReadable(
                storageRoot)
                || !Files.isWritable(
                        storageRoot)) {

            throw new IllegalArgumentException(
                    "supporting evidence storage root must be readable and writable");
        }
    }

    private Path targetPath(
            SupportingEvidenceStorageReference reference) {

        Path target =
                storageRoot
                        .resolve(
                                EVIDENCE_DIRECTORY)
                        .resolve(
                                reference.evidenceId()
                                        .value()
                                        .toString())
                        .resolve(
                                reference.sha256()
                                        + "."
                                        + reference.extension())
                        .normalize();

        if (!target.startsWith(
                storageRoot)) {

            throw unsafeStoragePath();
        }

        return target;
    }

    private static void ensureManagedDirectory(
            Path directory)
            throws IOException {

        if (Files.exists(
                directory,
                NOFOLLOW_LINKS)) {

            requireSafeDirectory(
                    directory);

            return;
        }

        try {
            Files.createDirectory(
                    directory);
        }
        catch (FileAlreadyExistsException exception) {
            requireSafeDirectory(
                    directory);
        }

        requireSafeDirectory(
                directory);
    }

    private static boolean validateExistingManagedDirectory(
            Path directory) {

        if (!Files.exists(
                directory,
                NOFOLLOW_LINKS)) {

            return false;
        }

        requireSafeDirectory(
                directory);

        return true;
    }

    private static void requireSafeDirectory(
            Path directory) {

        if (Files.isSymbolicLink(
                directory)
                || !Files.isDirectory(
                        directory,
                        NOFOLLOW_LINKS)) {

            throw unsafeStoragePath();
        }
    }

    private void validateInputSize(
            byte[] content) {

        if (content.length == 0) {
            throw new IllegalArgumentException(
                    "stored supporting evidence content must not be empty");
        }

        if (content.length > maximumContentBytes) {
            throw new IllegalArgumentException(
                    "stored supporting evidence content exceeds configured maximum");
        }
    }

    private static void verifyDigest(
            SupportingEvidenceStorageReference reference,
            byte[] content) {

        byte[] expectedDigest =
                HexFormat.of()
                        .parseHex(
                                reference.sha256());

        byte[] actualDigest =
                sha256(
                        content);

        if (!MessageDigest.isEqual(
                expectedDigest,
                actualDigest)) {

            throw integrityFailure();
        }
    }

    private static byte[] sha256(
            byte[] content) {

        try {
            return MessageDigest.getInstance(
                    "SHA-256")
                    .digest(
                            content);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception);
        }
    }

    private static void moveWithoutReplacement(
            Path source,
            Path target)
            throws IOException {

        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    source,
                    target);
        }
    }

    private static void deleteTemporaryFileQuietly(
            Path temporaryFile) {

        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(
                    temporaryFile);
        }
        catch (IOException ignored) {
            // The original storage failure remains authoritative.
        }
    }

    private static IllegalStateException unsafeStoragePath() {
        return new IllegalStateException(
                "supporting evidence storage path is unsafe");
    }

    private static IllegalStateException integrityFailure() {
        return new IllegalStateException(
                "stored supporting evidence content integrity verification failed");
    }

    private static IllegalStateException storageFailure(
            IOException exception) {

        return new IllegalStateException(
                "supporting evidence storage operation failed",
                exception);
    }

}