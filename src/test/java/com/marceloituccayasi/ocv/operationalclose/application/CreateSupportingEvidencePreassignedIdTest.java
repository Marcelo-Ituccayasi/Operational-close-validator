package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;

class CreateSupportingEvidencePreassignedIdTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "731e45ea-02ec-4385-86c2-897366100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "731e45ea-02ec-4385-86c2-897366100002");

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "731e45ea-02ec-4385-86c2-897366100003");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-23T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private static final String SHA256 =
            "0123456789abcdef0123456789abcdef"
                    + "0123456789abcdef0123456789abcdef";

    private static final String CONTENT_REFERENCE =
            "stored:evidence/"
                    + EVIDENCE_UUID
                    + "/"
                    + SHA256
                    + ".pdf";

    private final OperationalCloseLockRepository
            closeLockRepository =
                    mock(
                            OperationalCloseLockRepository.class);

    private final OperationalEventRevisionRepository
            eventRevisionRepository =
                    mock(
                            OperationalEventRevisionRepository.class);

    private final SupportingEvidenceRepository
            evidenceRepository =
                    mock(
                            SupportingEvidenceRepository.class);

    private final CurrentActorProvider
            currentActorProvider =
                    mock(
                            CurrentActorProvider.class);

    private final ApplicationClock
            applicationClock =
                    mock(
                            ApplicationClock.class);

    private final UuidGenerator
            uuidGenerator =
                    mock(
                            UuidGenerator.class);

    private final OperationalCloseRevisionRepository
            closeRevisionRepository =
                    mock(
                            OperationalCloseRevisionRepository.class);

    private final OperationalEventDependentResultInvalidator
            dependentResultInvalidator =
                    mock(
                            OperationalEventDependentResultInvalidator.class);

    private final TransactionRunner transactionRunner =
            new TransactionRunner() {

                @Override
                public <T> T execute(
                        Supplier<T> operation) {

                    return Objects.requireNonNull(
                            operation)
                            .get();
                }
            };

    private final OperationalDependencyRevisionCoordinator
            revisionCoordinator =
                    new OperationalDependencyRevisionCoordinator(
                            eventRevisionRepository,
                            closeRevisionRepository,
                            dependentResultInvalidator,
                            uuidGenerator);

    private final CreateSupportingEvidence useCase =
            new CreateSupportingEvidence(
                    closeLockRepository,
                    eventRevisionRepository,
                    evidenceRepository,
                    currentActorProvider,
                    applicationClock,
                    uuidGenerator,
                    transactionRunner,
                    revisionCoordinator);

    @Test
    void createsEvidenceUsingPreassignedIdentityWithoutGeneratingAnotherOne() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                AuditActor.RESPONSIBLE_USER_ID,
                                "responsible"));

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose()));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent()));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);

        CreateSupportingEvidenceResult result =
                useCase.executeWithEvidenceId(
                        command(),
                        EVIDENCE_UUID);

        ArgumentCaptor<SupportingEvidence>
                evidenceCaptor =
                        ArgumentCaptor.forClass(
                                SupportingEvidence.class);

        verify(
                evidenceRepository)
                .saveNew(
                        evidenceCaptor.capture());

        SupportingEvidence evidence =
                evidenceCaptor.getValue();

        assertThat(result.status())
                .isEqualTo(
                        CreateSupportingEvidenceResult.Status.CREATED);

        assertThat(result.evidenceId())
                .isEqualTo(
                        EVIDENCE_UUID);

        assertThat(
                evidence.id()
                        .value())
                .isEqualTo(
                        EVIDENCE_UUID);

        assertThat(evidence.contentReference())
                .isEqualTo(
                        CONTENT_REFERENCE);

        verify(
                dependentResultInvalidator)
                .invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                EVENT_ID));

        verifyNoInteractions(
                uuidGenerator,
                closeRevisionRepository);
    }

    private static CreateSupportingEvidenceCommand command() {
        return new CreateSupportingEvidenceCommand(
                CLOSE_UUID,
                EVENT_UUID,
                "Receipt",
                CONTENT_REFERENCE,
                new BigDecimal(
                        "80.0000"),
                LocalDate.of(
                        2026,
                        7,
                        23),
                "LEGIBLE");
    }

    private static OperationalClose operationalClose() {
        return new OperationalClose(
                CLOSE_ID,
                new OperationalPeriod(
                        LocalDate.of(
                                2026,
                                7,
                                1),
                        LocalDate.of(
                                2026,
                                7,
                                31)),
                new CurrencyCode(
                        "PEN"),
                new InitialBalance(
                        new BigDecimal(
                                "1000.0000")),
                OperationalCloseState.PREPARATION,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEvent operationalEvent() {
        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(
                                "80.0000")),
                new BigDecimal(
                        "-80.0000"),
                null,
                CREATED_AT.minusSeconds(
                        60),
                CREATED_AT,
                "Caja principal",
                "Gasto operativo",
                OperationalEventState.REGISTERED,
                true,
                false,
                1L,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}