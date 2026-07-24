package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.marceloituccayasi.ocv.operationalclose.application.port.SupportingEvidenceContentStorage;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

class CreateSupportingEvidenceWithStoredContentTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "de6f262d-73b6-4282-9cb3-129292100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "de6f262d-73b6-4282-9cb3-129292100002");

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "de6f262d-73b6-4282-9cb3-129292100003");

    private static final byte[] CONTENT =
            new byte[] {
                    1,
                    2,
                    3,
                    4,
                    5
            };

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

    private final SupportingEvidenceContentStorage
            contentStorage =
                    mock(
                            SupportingEvidenceContentStorage.class);

    private final CreateSupportingEvidence
            createSupportingEvidence =
                    mock(
                            CreateSupportingEvidence.class);

    private final CreateSupportingEvidenceWithStoredContent
            useCase =
                    new CreateSupportingEvidenceWithStoredContent(
                            uuidGenerator,
                            contentStorage,
                            createSupportingEvidence);

    @Test
    void storesContentBeforeCreatingEvidenceWithTheSameIdentity() {
        SupportingEvidenceStorageReference
                expectedReference =
                        expectedReference();

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID);

        when(
                createSupportingEvidence
                        .executeWithEvidenceId(
                                any(
                                        CreateSupportingEvidenceCommand.class),
                                eq(
                                        EVIDENCE_UUID)))
                .thenReturn(
                        CreateSupportingEvidenceResult.created(
                                EVIDENCE_UUID));

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        command(
                                CONTENT,
                                "pdf"));

        ArgumentCaptor<SupportingEvidenceStorageReference>
                referenceCaptor =
                        ArgumentCaptor.forClass(
                                SupportingEvidenceStorageReference.class);

        ArgumentCaptor<byte[]>
                contentCaptor =
                        ArgumentCaptor.forClass(
                                byte[].class);

        ArgumentCaptor<CreateSupportingEvidenceCommand>
                commandCaptor =
                        ArgumentCaptor.forClass(
                                CreateSupportingEvidenceCommand.class);

        verify(
                contentStorage)
                .store(
                        referenceCaptor.capture(),
                        contentCaptor.capture());

        verify(
                createSupportingEvidence)
                .executeWithEvidenceId(
                        commandCaptor.capture(),
                        eq(
                                EVIDENCE_UUID));

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status.CREATED);

        assertThat(result.evidenceId())
                .isEqualTo(
                        EVIDENCE_UUID);

        assertThat(referenceCaptor.getValue())
                .isEqualTo(
                        expectedReference);

        assertThat(contentCaptor.getValue())
                .containsExactly(
                        CONTENT);

        assertThat(
                commandCaptor.getValue()
                        .contentReference())
                .isEqualTo(
                        expectedReference.value());

        assertThat(
                commandCaptor.getValue()
                        .closeId())
                .isEqualTo(
                        CLOSE_UUID);

        assertThat(
                commandCaptor.getValue()
                        .eventId())
                .isEqualTo(
                        EVENT_UUID);

        InOrder order =
                inOrder(
                        uuidGenerator,
                        contentStorage,
                        createSupportingEvidence);

        order.verify(
                uuidGenerator)
                .next();

        order.verify(
                contentStorage)
                .store(
                        eq(
                                expectedReference),
                        any(
                                byte[].class));

        order.verify(
                createSupportingEvidence)
                .executeWithEvidenceId(
                        any(
                                CreateSupportingEvidenceCommand.class),
                        eq(
                                EVIDENCE_UUID));
    }

    @Test
    void deletesStoredContentWhenDatabaseCreationIsRejected() {
        SupportingEvidenceStorageReference
                expectedReference =
                        expectedReference();

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID);

        when(
                createSupportingEvidence
                        .executeWithEvidenceId(
                                any(
                                        CreateSupportingEvidenceCommand.class),
                                eq(
                                        EVIDENCE_UUID)))
                .thenReturn(
                        CreateSupportingEvidenceResult
                                .closeNotFound());

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        command(
                                CONTENT,
                                "pdf"));

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .CLOSE_NOT_FOUND);

        InOrder order =
                inOrder(
                        contentStorage,
                        createSupportingEvidence);

        order.verify(
                contentStorage)
                .store(
                        eq(
                                expectedReference),
                        any(
                                byte[].class));

        order.verify(
                createSupportingEvidence)
                .executeWithEvidenceId(
                        any(
                                CreateSupportingEvidenceCommand.class),
                        eq(
                                EVIDENCE_UUID));

        order.verify(
                contentStorage)
                .delete(
                        expectedReference);
    }

    @Test
    void deletesStoredContentWhenDatabaseCreationThrows() {
        SupportingEvidenceStorageReference
                expectedReference =
                        expectedReference();

        IllegalStateException operationFailure =
                new IllegalStateException(
                        "database failure");

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID);

        when(
                createSupportingEvidence
                        .executeWithEvidenceId(
                                any(
                                        CreateSupportingEvidenceCommand.class),
                                eq(
                                        EVIDENCE_UUID)))
                .thenThrow(
                        operationFailure);

        assertThatThrownBy(
                () ->
                    useCase.execute(
                            command(
                                    CONTENT,
                                    "pdf")))
                .isSameAs(
                        operationFailure);

        verify(
                contentStorage)
                .delete(
                        expectedReference);
    }

    @Test
    void reportsManualCleanupWhenCompensationFails() {
        SupportingEvidenceStorageReference
                expectedReference =
                        expectedReference();

        IllegalStateException compensationFailure =
                new IllegalStateException(
                        "filesystem failure");

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID);

        when(
                createSupportingEvidence
                        .executeWithEvidenceId(
                                any(
                                        CreateSupportingEvidenceCommand.class),
                                eq(
                                        EVIDENCE_UUID)))
                .thenReturn(
                        CreateSupportingEvidenceResult
                                .eventNotFound());

        doThrow(
                compensationFailure)
                .when(
                        contentStorage)
                .delete(
                        expectedReference);

        assertThatThrownBy(
                () ->
                    useCase.execute(
                            command(
                                    CONTENT,
                                    "pdf")))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "supporting evidence storage compensation failed; manual cleanup is required")
                .cause()
                .isSameAs(
                        compensationFailure);
    }

    @Test
    void rejectsEmptyContentBeforeGeneratingIdentityOrWriting() {
        CreateSupportingEvidenceResult result =
                useCase.execute(
                        command(
                                new byte[0],
                                "pdf"));

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .INVALID_INPUT);

        assertThat(result.message())
                .isEqualTo(
                        "content must not be empty");

        verifyNoInteractions(
                uuidGenerator,
                contentStorage,
                createSupportingEvidence);
    }

    @Test
    void rejectsUnsupportedExtensionBeforeWritingContent() {
        when(
                uuidGenerator.next())
                .thenReturn(
                        EVIDENCE_UUID);

        CreateSupportingEvidenceResult result =
                useCase.execute(
                        command(
                                CONTENT,
                                "exe"));

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status
                                .INVALID_INPUT);

        assertThat(result.message())
                .isEqualTo(
                        "stored evidence extension is not allowed");

        verify(
                uuidGenerator)
                .next();

        verifyNoInteractions(
                contentStorage,
                createSupportingEvidence);
    }

    @Test
    void commandDefensivelyCopiesBinaryContent() {
        byte[] original =
                new byte[] {
                        8,
                        9,
                        10
                };

        CreateSupportingEvidenceWithStoredContentCommand command =
                command(
                        original,
                        "png");

        original[0] =
                99;

        assertThat(command.content())
                .containsExactly(
                        8,
                        9,
                        10);

        byte[] recovered =
                command.content();

        recovered[1] =
                88;

        assertThat(command.content())
                .containsExactly(
                        8,
                        9,
                        10);
    }

    private static CreateSupportingEvidenceWithStoredContentCommand
            command(
                    byte[] content,
                    String extension) {

        return new CreateSupportingEvidenceWithStoredContentCommand(
                CLOSE_UUID,
                EVENT_UUID,
                "Receipt",
                content,
                extension,
                new BigDecimal(
                        "80.0000"),
                LocalDate.of(
                        2026,
                        7,
                        23),
                "LEGIBLE");
    }

    private static SupportingEvidenceStorageReference
            expectedReference() {

        return SupportingEvidenceStorageReference.create(
                new SupportingEvidenceId(
                        EVIDENCE_UUID),
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

}