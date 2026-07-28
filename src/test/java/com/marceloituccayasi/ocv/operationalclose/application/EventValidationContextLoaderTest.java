package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventAuthorizationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.SupportingEvidenceRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceLegibilityStatus;

class EventValidationContextLoaderTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "37c5c2f6-4606-40cc-826f-f75d78210001"));

    private static final OperationalCloseId OTHER_CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "37c5c2f6-4606-40cc-826f-f75d78210002"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "37c5c2f6-4606-40cc-826f-f75d78210003"));

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-28T08:00:00Z");

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

    private final EventValidationContextLoader loader =
            new EventValidationContextLoader(
                    eventRepository,
                    evidenceRepository,
                    authorizationRepository);

    @Test
    void loadsOwnedEventWithEvidenceAndAuthorizations() {
        OperationalEvent event =
                event(
                        CLOSE_ID);

        SupportingEvidence evidence =
                evidence();

        EventAuthorization authorization =
                authorization();

        when(
                eventRepository.findById(
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                event));

        when(
                evidenceRepository
                        .findAllByEventIdOrderByEvidenceDateDescending(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                evidence));

        when(
                authorizationRepository
                        .findAllByEventIdOrderByAuthorizedAtDescending(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                authorization));

        Optional<EventValidationContext> loadedContext =
                loader.load(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(loadedContext)
                .isPresent();

        assertThat(
                loadedContext.orElseThrow()
                        .event())
                .isEqualTo(
                        event);

        assertThat(
                loadedContext.orElseThrow()
                        .supportingEvidence())
                .containsExactly(
                        evidence);

        assertThat(
                loadedContext.orElseThrow()
                        .authorizations())
                .containsExactly(
                        authorization);

        verify(
                eventRepository)
                .findById(
                        EVENT_ID);

        verify(
                evidenceRepository)
                .findAllByEventIdOrderByEvidenceDateDescending(
                        EVENT_ID);

        verify(
                authorizationRepository)
                .findAllByEventIdOrderByAuthorizedAtDescending(
                        EVENT_ID);
    }

    @Test
    void returnsEmptyWhenEventDoesNotExist() {
        when(
                eventRepository.findById(
                        EVENT_ID))
                .thenReturn(
                        Optional.empty());

        assertThat(
                loader.load(
                        CLOSE_ID,
                        EVENT_ID))
                .isEmpty();

        verify(
                eventRepository)
                .findById(
                        EVENT_ID);

        verifyNoInteractions(
                evidenceRepository,
                authorizationRepository);
    }

    @Test
    void returnsEmptyWhenEventBelongsToAnotherClose() {
        when(
                eventRepository.findById(
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                event(
                                        OTHER_CLOSE_ID)));

        assertThat(
                loader.load(
                        CLOSE_ID,
                        EVENT_ID))
                .isEmpty();

        verify(
                eventRepository)
                .findById(
                        EVENT_ID);

        verifyNoInteractions(
                evidenceRepository,
                authorizationRepository);
    }

    @Test
    void rejectsNullScopeBeforeReadingRepositories() {
        assertThatThrownBy(
                () -> loader.load(
                        null,
                        EVENT_ID))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "closeId must not be null");

        assertThatThrownBy(
                () -> loader.load(
                        CLOSE_ID,
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "eventId must not be null");

        verifyNoInteractions(
                eventRepository,
                evidenceRepository,
                authorizationRepository);
    }

    private static OperationalEvent event(
            OperationalCloseId closeId) {

        return OperationalEvent.create(
                EVENT_ID,
                closeId,
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal(
                                "50.0000")),
                CREATED_AT.minusSeconds(
                        60L),
                "Caja principal",
                "Evento para cargar contexto de validación",
                true,
                true,
                CREATED_AT,
                ACTOR);
    }

    private static SupportingEvidence evidence() {
        return SupportingEvidence.create(
                new SupportingEvidenceId(
                        UUID.fromString(
                                "37c5c2f6-4606-40cc-826f-f75d78210004")),
                EVENT_ID,
                "RECEIPT",
                "reference:validation-context-evidence",
                new BigDecimal(
                        "50.0000"),
                LocalDate.of(
                        2026,
                        7,
                        28),
                SupportingEvidenceLegibilityStatus.LEGIBLE,
                CREATED_AT.plusSeconds(
                        60L),
                ACTOR);
    }

    private static EventAuthorization authorization() {
        return EventAuthorization.create(
                new EventAuthorizationId(
                        UUID.fromString(
                                "37c5c2f6-4606-40cc-826f-f75d78210005")),
                EVENT_ID,
                "Gerencia",
                "Autorización formal",
                CREATED_AT,
                "AUTH-CONTEXT-001",
                CREATED_AT.plusSeconds(
                        60L),
                ACTOR);
    }

}