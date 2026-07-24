package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

/**
 * Retrieves verified Supporting Evidence content within its persisted ownership
 * scope.
 *
 * <p>The database lookup completes before physical storage is accessed.
 * Opaque {@code reference:} evidence has no downloadable application-managed
 * content and therefore produces a not-found result.
 */
public final class GetSupportingEvidenceContent {

    private static final String OPAQUE_REFERENCE_PREFIX =
            "reference:";

    private final SupportingEvidenceRepository
            evidenceRepository;

    private final SupportingEvidenceContentStorage
            contentStorage;

    private final TransactionRunner
            transactionRunner;

    public GetSupportingEvidenceContent(
            SupportingEvidenceRepository evidenceRepository,
            SupportingEvidenceContentStorage contentStorage,
            TransactionRunner transactionRunner) {

        this.evidenceRepository =
                Objects.requireNonNull(
                        evidenceRepository);

        this.contentStorage =
                Objects.requireNonNull(
                        contentStorage);

        this.transactionRunner =
                Objects.requireNonNull(
                        transactionRunner);
    }

    public GetSupportingEvidenceContentResult execute(
            UUID closeId,
            UUID eventId,
            UUID evidenceId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        Objects.requireNonNull(
                evidenceId,
                "evidenceId must not be null");

        OperationalCloseId requestedCloseId =
                new OperationalCloseId(
                        closeId);

        OperationalEventId requestedEventId =
                new OperationalEventId(
                        eventId);

        SupportingEvidenceId requestedEvidenceId =
                new SupportingEvidenceId(
                        evidenceId);

        Optional<SupportingEvidence> persistedEvidence =
                transactionRunner.execute(
                        () -> evidenceRepository.findById(
                                requestedCloseId,
                                requestedEventId,
                                requestedEvidenceId));

        if (persistedEvidence.isEmpty()) {
            return GetSupportingEvidenceContentResult
                    .notFound();
        }

        SupportingEvidence evidence =
                persistedEvidence.orElseThrow();

        String contentReference =
                evidence.contentReference();

        if (contentReference.startsWith(
                OPAQUE_REFERENCE_PREFIX)) {

            return GetSupportingEvidenceContentResult
                    .notFound();
        }

        SupportingEvidenceStorageReference
                storageReference =
                        parsePersistedReference(
                                contentReference);

        if (!storageReference.evidenceId()
                .equals(
                        evidence.id())) {

            throw new IllegalStateException(
                    "persisted supporting evidence content reference does not match evidence identity");
        }

        Optional<StoredSupportingEvidenceContent>
                storedContent =
                        contentStorage.find(
                                storageReference);

        if (storedContent.isEmpty()) {
            return GetSupportingEvidenceContentResult
                    .notFound();
        }

        StoredSupportingEvidenceContent content =
                storedContent.orElseThrow();

        if (!content.reference()
                .equals(
                        storageReference)) {

            throw new IllegalStateException(
                    "stored supporting evidence content reference is inconsistent");
        }

        return GetSupportingEvidenceContentResult
                .found(
                        content);
    }

    private static SupportingEvidenceStorageReference
            parsePersistedReference(
                    String contentReference) {

        try {
            return SupportingEvidenceStorageReference
                    .parse(
                            contentReference);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "persisted supporting evidence content reference is invalid",
                    exception);
        }
    }

}