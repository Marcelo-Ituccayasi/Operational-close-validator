package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

class GetSupportingEvidenceContentTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "e02a6d76-b19b-42ec-99e4-891154100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "e02a6d76-b19b-42ec-99e4-891154100002");

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "e02a6d76-b19b-42ec-99e4-891154100003");

    private static final UUID OTHER_EVIDENCE_UUID =
            UUID.fromString(
                    "e02a6d76-b19b-42ec-99e4-891154100004");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final SupportingEvidenceId EVIDENCE_ID =
            new SupportingEvidenceId(
                    EVIDENCE_UUID);

    private static final byte[] CONTENT =
            new byte[] {
                    1,
                    2,
                    3,
                    4
            };

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-24T08:00:00Z");

    private static final Instant DEACTIVATED_AT =
            Instant.parse(
                    "2026-07-24T09:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final SupportingEvidenceRepository
            evidenceRepository =
                    mock(
                            SupportingEvidenceRepository.class);

    private final SupportingEvidenceContentStorage
            contentStorage =
                    mock(
                            SupportingEvidenceContentStorage.class);

    private final RecordingTransactionRunner
            transactionRunner =
                    new RecordingTransactionRunner();

    private final GetSupportingEvidenceContent useCase =
            new GetSupportingEvidenceContent(
                    evidenceRepository,
                    contentStorage,
                    transactionRunner);

    @Test
    void retrievesVerifiedContentAfterDatabaseTransactionCompletes() {
        SupportingEvidenceStorageReference reference =
                storedReference(
                        EVIDENCE_ID);

        SupportingEvidence evidence =
                activeEvidence(
                        reference.value());

        StoredSupportingEvidenceContent storedContent =
                new StoredSupportingEvidenceContent(
                        reference,
                        CONTENT);

        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenAnswer(invocation -> {
                    assertThat(
                            transactionRunner
                                    .insideTransaction())
                            .isTrue();

                    return Optional.of(
                            evidence);
                });

        when(
                contentStorage.find(
                        reference))
                .thenAnswer(invocation -> {
                    assertThat(
                            transactionRunner
                                    .insideTransaction())
                            .isFalse();

                    return Optional.of(
                            storedContent);
                });

        GetSupportingEvidenceContentResult result =
                useCase.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetSupportingEvidenceContentResult.Status.FOUND);

        assertThat(result.content())
                .isEqualTo(
                        storedContent);

        assertThat(result.content().content())
                .containsExactly(
                        CONTENT);
    }

    @Test
    void returnsNotFoundWhenEvidenceIsOutsideRequestedOwnershipScope() {
        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.empty());

        GetSupportingEvidenceContentResult result =
                useCase.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetSupportingEvidenceContentResult.Status
                                .NOT_FOUND);

        assertThat(result.content())
                .isNull();

        verifyNoInteractions(
                contentStorage);
    }

    @Test
    void returnsNotFoundForOpaqueBusinessReference() {
        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                activeEvidence(
                                        "reference:receipt-001")));

        GetSupportingEvidenceContentResult result =
                useCase.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetSupportingEvidenceContentResult.Status
                                .NOT_FOUND);

        verifyNoInteractions(
                contentStorage);
    }

    @Test
    void returnsNotFoundWhenManagedContentIsMissing() {
        SupportingEvidenceStorageReference reference =
                storedReference(
                        EVIDENCE_ID);

        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                activeEvidence(
                                        reference.value())));

        when(
                contentStorage.find(
                        reference))
                .thenReturn(
                        Optional.empty());

        GetSupportingEvidenceContentResult result =
                useCase.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetSupportingEvidenceContentResult.Status
                                .NOT_FOUND);
    }

    @Test
    void retrievesContentForDeactivatedEvidence() {
        SupportingEvidenceStorageReference reference =
                storedReference(
                        EVIDENCE_ID);

        SupportingEvidence inactiveEvidence =
                inactiveEvidence(
                        reference.value());

        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                inactiveEvidence));

        when(
                contentStorage.find(
                        reference))
                .thenReturn(
                        Optional.of(
                                new StoredSupportingEvidenceContent(
                                        reference,
                                        CONTENT)));

        GetSupportingEvidenceContentResult result =
                useCase.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetSupportingEvidenceContentResult.Status.FOUND);

        verify(
                contentStorage)
                .find(
                        reference);
    }

    @Test
    void rejectsMalformedPersistedStoredReferenceAsTechnicalFailure() {
        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                activeEvidence(
                                        "stored:evidence/receipt.pdf")));

        assertThatThrownBy(
                () ->
                    useCase.execute(
                            CLOSE_UUID,
                            EVENT_UUID,
                            EVIDENCE_UUID))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "persisted supporting evidence content reference is invalid");

        verifyNoInteractions(
                contentStorage);
    }

    @Test
    void rejectsStoredReferenceWhoseIdentityDoesNotMatchEvidence() {
        SupportingEvidenceStorageReference otherReference =
                storedReference(
                        new SupportingEvidenceId(
                                OTHER_EVIDENCE_UUID));

        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                activeEvidence(
                                        otherReference.value())));

        assertThatThrownBy(
                () ->
                    useCase.execute(
                            CLOSE_UUID,
                            EVENT_UUID,
                            EVIDENCE_UUID))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "persisted supporting evidence content reference does not match evidence identity");

        verifyNoInteractions(
                contentStorage);
    }

    @Test
    void rejectsContentReturnedForAReferenceOtherThanRequested() {
        SupportingEvidenceStorageReference requestedReference =
                storedReference(
                        EVIDENCE_ID);

        SupportingEvidenceStorageReference returnedReference =
                storedReference(
                        new SupportingEvidenceId(
                                OTHER_EVIDENCE_UUID));

        when(
                evidenceRepository.findById(
                        CLOSE_ID,
                        EVENT_ID,
                        EVIDENCE_ID))
                .thenReturn(
                        Optional.of(
                                activeEvidence(
                                        requestedReference.value())));

        when(
                contentStorage.find(
                        requestedReference))
                .thenReturn(
                        Optional.of(
                                new StoredSupportingEvidenceContent(
                                        returnedReference,
                                        CONTENT)));

        assertThatThrownBy(
                () ->
                    useCase.execute(
                            CLOSE_UUID,
                            EVENT_UUID,
                            EVIDENCE_UUID))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "stored supporting evidence content reference is inconsistent");
    }

    private static SupportingEvidence activeEvidence(
            String contentReference) {

        return SupportingEvidence.create(
                EVIDENCE_ID,
                EVENT_ID,
                "RECEIPT",
                contentReference,
                new BigDecimal(
                        "125.5000"),
                LocalDate.of(
                        2026,
                        7,
                        24),
                SupportingEvidenceLegibilityStatus.LEGIBLE,
                CREATED_AT,
                ACTOR);
    }

    private static SupportingEvidence inactiveEvidence(
            String contentReference) {

        SupportingEvidence activeEvidence =
                activeEvidence(
                        contentReference);

        return new SupportingEvidence(
                activeEvidence.id(),
                activeEvidence.eventId(),
                activeEvidence.evidenceType(),
                activeEvidence.contentReference(),
                activeEvidence.supportedAmount(),
                activeEvidence.evidenceDate(),
                activeEvidence.legibilityStatus(),
                false,
                2L,
                activeEvidence.createdAt(),
                activeEvidence.createdBy(),
                DEACTIVATED_AT,
                ACTOR,
                DEACTIVATED_AT);
    }

    private static SupportingEvidenceStorageReference
            storedReference(
                    SupportingEvidenceId evidenceId) {

        return SupportingEvidenceStorageReference.create(
                evidenceId,
                sha256Hex(
                        CONTENT),
                "pdf");
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

    private static final class RecordingTransactionRunner
            implements TransactionRunner {

        private boolean insideTransaction;

        @Override
        public <T> T execute(
                Supplier<T> operation) {

            Objects.requireNonNull(
                    operation,
                    "operation must not be null");

            insideTransaction =
                    true;

            try {
                return operation.get();
            }
            finally {
                insideTransaction =
                        false;
            }
        }

        boolean insideTransaction() {
            return insideTransaction;
        }

    }

}