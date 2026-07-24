package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

class GetOperationalEventSupportingInformationTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100001");

    private static final UUID OTHER_CLOSE_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100002");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100003");

    private static final UUID ACTIVE_EVIDENCE_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100004");

    private static final UUID HISTORICAL_EVIDENCE_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100005");

    private static final UUID ACTIVE_AUTHORIZATION_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100006");

    private static final UUID HISTORICAL_AUTHORIZATION_UUID =
            UUID.fromString(
                    "af7ea480-c5c6-4859-bab5-e80777100007");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

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

    private final OperationalEventRepository
            eventRepository =
                    mock(
                            OperationalEventRepository.class);

    private final SupportingEvidenceRepository
            evidenceRepository =
                    mock(
                            SupportingEvidenceRepository.class);

    private final EventAuthorizationRepository
            authorizationRepository =
                    mock(
                            EventAuthorizationRepository.class);

    private final TransactionRunner transactionRunner =
            new TransactionRunner() {

                @Override
                public <T> T execute(
                        Supplier<T> operation) {

                    return Objects.requireNonNull(
                            operation,
                            "operation must not be null")
                            .get();
                }
            };

    private final GetOperationalEventSupportingInformation useCase =
            new GetOperationalEventSupportingInformation(
                    eventRepository,
                    evidenceRepository,
                    authorizationRepository,
                    transactionRunner);

    @Test
    void returnsCurrentAndHistoricalSupportingInformationInRepositoryOrder() {
        SupportingEvidence activeEvidence =
                activeEvidence();

        SupportingEvidence historicalEvidence =
                historicalEvidence();

        EventAuthorization activeAuthorization =
                activeAuthorization();

        EventAuthorization historicalAuthorization =
                historicalAuthorization();

        when(
                eventRepository.findById(
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent()));

        when(
                evidenceRepository
                        .findAllByEventIdOrderByEvidenceDateDescending(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                activeEvidence,
                                historicalEvidence));

        when(
                authorizationRepository
                        .findAllByEventIdOrderByAuthorizedAtDescending(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                activeAuthorization,
                                historicalAuthorization));

        GetOperationalEventSupportingInformationResult result =
                useCase.execute(
                        CLOSE_UUID,
                        EVENT_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalEventSupportingInformationResult
                                .Status.FOUND);

        assertThat(result.supportingEvidence())
                .hasSize(2);

        assertThat(
                result.supportingEvidence()
                        .get(0)
                        .id())
                .isEqualTo(
                        ACTIVE_EVIDENCE_UUID);

        assertThat(
                result.supportingEvidence()
                        .get(0)
                        .managedContent())
                .isTrue();

        assertThat(
                result.supportingEvidence()
                        .get(0)
                        .active())
                .isTrue();

        assertThat(
                result.supportingEvidence()
                        .get(1)
                        .id())
                .isEqualTo(
                        HISTORICAL_EVIDENCE_UUID);

        assertThat(
                result.supportingEvidence()
                        .get(1)
                        .managedContent())
                .isFalse();

        assertThat(
                result.supportingEvidence()
                        .get(1)
                        .active())
                .isFalse();

        assertThat(
                result.supportingEvidence()
                        .get(1)
                        .deactivatedAt())
                .isEqualTo(
                        DEACTIVATED_AT);

        assertThat(result.authorizations())
                .hasSize(2);

        assertThat(
                result.authorizations()
                        .get(0)
                        .id())
                .isEqualTo(
                        ACTIVE_AUTHORIZATION_UUID);

        assertThat(
                result.authorizations()
                        .get(0)
                        .active())
                .isTrue();

        assertThat(
                result.authorizations()
                        .get(1)
                        .id())
                .isEqualTo(
                        HISTORICAL_AUTHORIZATION_UUID);

        assertThat(
                result.authorizations()
                        .get(1)
                        .active())
                .isFalse();

        assertThat(
                result.authorizations()
                        .get(1)
                        .deactivatedAt())
                .isEqualTo(
                        DEACTIVATED_AT);
    }

    @Test
    void returnsNotFoundWithoutQueryingDependentsWhenEventIsOutsideClose() {
        when(
                eventRepository.findById(
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent()));

        GetOperationalEventSupportingInformationResult result =
                useCase.execute(
                        OTHER_CLOSE_UUID,
                        EVENT_UUID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalEventSupportingInformationResult
                                .Status.NOT_FOUND);

        assertThat(result.supportingEvidence())
                .isEmpty();

        assertThat(result.authorizations())
                .isEmpty();

        verifyNoInteractions(
                evidenceRepository,
                authorizationRepository);
    }

    private static OperationalEvent operationalEvent() {
        return OperationalEvent.create(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(
                                "80.0000")),
                CREATED_AT.minusSeconds(
                        60),
                "Caja principal",
                "Gasto operativo",
                true,
                true,
                CREATED_AT,
                ACTOR);
    }

    private static SupportingEvidence activeEvidence() {
        return SupportingEvidence.create(
                new SupportingEvidenceId(
                        ACTIVE_EVIDENCE_UUID),
                EVENT_ID,
                "RECEIPT",
                "stored:evidence/"
                        + ACTIVE_EVIDENCE_UUID
                        + "/"
                        + "0123456789abcdef0123456789abcdef"
                        + "0123456789abcdef0123456789abcdef"
                        + ".pdf",
                new BigDecimal(
                        "80.0000"),
                LocalDate.of(
                        2026,
                        7,
                        24),
                SupportingEvidenceLegibilityStatus.LEGIBLE,
                CREATED_AT,
                ACTOR);
    }

    private static SupportingEvidence historicalEvidence() {
        return new SupportingEvidence(
                new SupportingEvidenceId(
                        HISTORICAL_EVIDENCE_UUID),
                EVENT_ID,
                "RECEIPT",
                "reference:historical-receipt",
                new BigDecimal(
                        "80.0000"),
                LocalDate.of(
                        2026,
                        7,
                        23),
                SupportingEvidenceLegibilityStatus.UNVERIFIED,
                false,
                2L,
                CREATED_AT,
                ACTOR,
                DEACTIVATED_AT,
                ACTOR,
                DEACTIVATED_AT);
    }

    private static EventAuthorization activeAuthorization() {
        return EventAuthorization.create(
                new EventAuthorizationId(
                        ACTIVE_AUTHORIZATION_UUID),
                EVENT_ID,
                "Supervisor de caja",
                "Operación excepcional aprobada",
                CREATED_AT.minusSeconds(
                        120),
                "AUTH-001",
                CREATED_AT,
                ACTOR);
    }

    private static EventAuthorization historicalAuthorization() {
        return new EventAuthorization(
                new EventAuthorizationId(
                        HISTORICAL_AUTHORIZATION_UUID),
                EVENT_ID,
                "Gerencia",
                "Autorización histórica",
                CREATED_AT.minusSeconds(
                        240),
                "AUTH-HIST-001",
                false,
                2L,
                CREATED_AT,
                ACTOR,
                DEACTIVATED_AT,
                ACTOR,
                DEACTIVATED_AT);
    }

}