package com.marceloituccayasi.ocv.operationalclose.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

/**
 * Coordinates physical content storage outside the database transaction and
 * the subsequent transactional creation of Supporting Evidence.
 *
 * <p>If the database link is not created, the stored content is deleted as a
 * compensation action.
 */
public final class CreateSupportingEvidenceWithStoredContent {

    private final UuidGenerator
            uuidGenerator;

    private final SupportingEvidenceContentStorage
            contentStorage;

    private final CreateSupportingEvidence
            createSupportingEvidence;

    public CreateSupportingEvidenceWithStoredContent(
            UuidGenerator uuidGenerator,
            SupportingEvidenceContentStorage contentStorage,
            CreateSupportingEvidence createSupportingEvidence) {

        this.uuidGenerator =
                Objects.requireNonNull(
                        uuidGenerator);

        this.contentStorage =
                Objects.requireNonNull(
                        contentStorage);

        this.createSupportingEvidence =
                Objects.requireNonNull(
                        createSupportingEvidence);
    }

    public CreateSupportingEvidenceResult execute(
            CreateSupportingEvidenceWithStoredContentCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        byte[] content =
                command.content();

        if (content == null) {
            return CreateSupportingEvidenceResult
                    .invalidInput(
                            "content must not be null");
        }

        if (content.length == 0) {
            return CreateSupportingEvidenceResult
                    .invalidInput(
                            "content must not be empty");
        }

        UUID evidenceUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated evidence UUID must not be null");

        SupportingEvidenceStorageReference
                storageReference;

        try {
            storageReference =
                    SupportingEvidenceStorageReference.create(
                            new SupportingEvidenceId(
                                    evidenceUuid),
                            sha256Hex(
                                    content),
                            command.extension());
        }
        catch (IllegalArgumentException exception) {
            return CreateSupportingEvidenceResult
                    .invalidInput(
                            exception.getMessage());
        }

        contentStorage.store(
                storageReference,
                content);

        CreateSupportingEvidenceCommand
                createCommand =
                        new CreateSupportingEvidenceCommand(
                                command.closeId(),
                                command.eventId(),
                                command.evidenceType(),
                                storageReference.value(),
                                command.supportedAmount(),
                                command.evidenceDate(),
                                command.legibilityStatus());

        CreateSupportingEvidenceResult result;

        try {
            result =
                    Objects.requireNonNull(
                            createSupportingEvidence
                                    .executeWithEvidenceId(
                                            createCommand,
                                            evidenceUuid),
                            "create Supporting Evidence result must not be null");
        }
        catch (RuntimeException exception) {
            compensate(
                    storageReference,
                    exception);

            throw exception;
        }

        if (result.status()
                != CreateSupportingEvidenceResult.Status.CREATED) {

            compensate(
                    storageReference,
                    null);
        }

        return result;
    }

    private void compensate(
            SupportingEvidenceStorageReference storageReference,
            RuntimeException operationFailure) {

        try {
            contentStorage.delete(
                    storageReference);
        }
        catch (RuntimeException compensationFailure) {
            IllegalStateException failure =
                    new IllegalStateException(
                            "supporting evidence storage compensation failed; manual cleanup is required",
                            compensationFailure);

            if (operationFailure != null) {
                failure.addSuppressed(
                        operationFailure);
            }

            throw failure;
        }
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
                    "SHA-256 algorithm is not available",
                    exception);
        }
    }

}