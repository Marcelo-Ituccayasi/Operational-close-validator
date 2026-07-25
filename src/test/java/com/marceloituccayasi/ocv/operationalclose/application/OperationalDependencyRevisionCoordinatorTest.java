package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalDependencyRevision;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class OperationalDependencyRevisionCoordinatorTest {

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    UUID.fromString(
                            "75614c27-465e-4566-a42d-829113100001"));

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    UUID.fromString(
                            "75614c27-465e-4566-a42d-829113100002"));

    private static final UUID EVENT_TRANSITION_ID =
            UUID.fromString(
                    "75614c27-465e-4566-a42d-829113100003");

    private static final UUID CLOSE_TRANSITION_ID =
            UUID.fromString(
                    "75614c27-465e-4566-a42d-829113100004");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-23T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-23T10:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalEventRevisionRepository
            eventRevisionRepository =
                    mock(
                            OperationalEventRevisionRepository.class);

    private final OperationalCloseRevisionRepository
            closeRevisionRepository =
                    mock(
                            OperationalCloseRevisionRepository.class);

    private final OperationalEventDependentResultInvalidator
            dependentResultInvalidator =
                    mock(
                            OperationalEventDependentResultInvalidator.class);

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

    private final OperationalDependencyRevisionCoordinator coordinator =
            new OperationalDependencyRevisionCoordinator(
                    eventRevisionRepository,
                    closeRevisionRepository,
                    dependentResultInvalidator,
                    uuidGenerator);

    @Test
    void incrementsEventRevisionWithoutTransitionsWhenStatesRemainUnchanged() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.PREPARATION);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        OperationalDependencyRevision result =
                coordinator.applyAndPersist(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR);

        assertThat(
                result.revisedEvent()
                        .dataRevision())
                .isEqualTo(2L);

        assertThat(
                result.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(result.revisedClose())
                .isSameAs(
                        operationalClose);

        verify(
                dependentResultInvalidator)
                .invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                EVENT_ID));

        verify(
                eventRevisionRepository)
                .saveRevision(
                        result.revisedEvent());

        verify(
                eventRevisionRepository,
                never())
                .appendStateTransition(
                        any());

        verifyNoInteractions(
                closeRevisionRepository,
                uuidGenerator);
    }

    @Test
    void persistsEventAndCloseTransitionsWhenValidatedStatesChange() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.VALIDATED);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.VALIDATED);

        when(
                uuidGenerator.next())
                .thenReturn(
                        EVENT_TRANSITION_ID,
                        CLOSE_TRANSITION_ID);

        OperationalDependencyRevision result =
                coordinator.applyAndPersist(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR);

        ArgumentCaptor<EventStateTransition>
                eventTransitionCaptor =
                        ArgumentCaptor.forClass(
                                EventStateTransition.class);

        ArgumentCaptor<CloseStateTransition>
                closeTransitionCaptor =
                        ArgumentCaptor.forClass(
                                CloseStateTransition.class);

        verify(
                eventRevisionRepository)
                .appendStateTransition(
                        eventTransitionCaptor.capture());

        verify(
                closeRevisionRepository)
                .appendStateTransition(
                        closeTransitionCaptor.capture());

        EventStateTransition eventTransition =
                eventTransitionCaptor.getValue();

        CloseStateTransition closeTransition =
                closeTransitionCaptor.getValue();

        assertThat(
                result.revisedEvent()
                        .state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(
                result.revisedClose()
                        .state())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        assertThat(
                eventTransition.id()
                        .value())
                .isEqualTo(
                        EVENT_TRANSITION_ID);

        assertThat(eventTransition.eventId())
                .isEqualTo(
                        EVENT_ID);

        assertThat(eventTransition.fromState())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(eventTransition.toState())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(eventTransition.causeCode())
                .isEqualTo(
                        OperationalDependencyRevisionCoordinator
                                .DEPENDENT_DATA_REVISED);

        assertThat(eventTransition.occurredAt())
                .isEqualTo(
                        REVISED_AT);

        assertThat(eventTransition.actor())
                .isEqualTo(
                        ACTOR);

        assertThat(
                closeTransition.id()
                        .value())
                .isEqualTo(
                        CLOSE_TRANSITION_ID);

        assertThat(closeTransition.closeId())
                .isEqualTo(
                        CLOSE_ID);

        assertThat(closeTransition.fromState())
                .isEqualTo(
                        OperationalCloseState.VALIDATED);

        assertThat(closeTransition.toState())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        assertThat(closeTransition.causeCode())
                .isEqualTo(
                        OperationalDependencyRevisionCoordinator
                                .DEPENDENT_DATA_REVISED);

        assertThat(closeTransition.occurredAt())
                .isEqualTo(
                        REVISED_AT);

        assertThat(closeTransition.actor())
                .isEqualTo(
                        ACTOR);

        InOrder persistenceOrder =
                inOrder(
                        dependentResultInvalidator,
                        eventRevisionRepository,
                        closeRevisionRepository);

        persistenceOrder.verify(
                dependentResultInvalidator)
                .invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                EVENT_ID));

        persistenceOrder.verify(
                eventRevisionRepository)
                .saveRevision(
                        result.revisedEvent());

        persistenceOrder.verify(
                eventRevisionRepository)
                .appendStateTransition(
                        eventTransition);

        persistenceOrder.verify(
                closeRevisionRepository)
                .saveRevision(
                        result.revisedClose());

        persistenceOrder.verify(
                closeRevisionRepository)
                .appendStateTransition(
                        closeTransition);
    }

    @Test
    void doesNotWriteWhenInvalidationFails() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.VALIDATED);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.VALIDATED);

        doThrow(
                new IllegalStateException(
                        "invalidation failure"))
                .when(
                        dependentResultInvalidator)
                .invalidateForRevisions(
                        CLOSE_ID,
                        List.of(
                                EVENT_ID));

        assertThatThrownBy(
                () -> coordinator.applyAndPersist(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "invalidation failure");

        verify(
                eventRevisionRepository,
                never())
                .saveRevision(
                        any());

        verify(
                eventRevisionRepository,
                never())
                .appendStateTransition(
                        any());

        verifyNoInteractions(
                closeRevisionRepository,
                uuidGenerator);
    }

    @Test
    void rejectsSentCloseBeforeInvalidationOrPersistence() {
        OperationalClose operationalClose =
                closeWithState(
                        OperationalCloseState.SENT_TO_ACCOUNTING);

        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.VALIDATED);

        assertThatThrownBy(
                () -> coordinator.applyAndPersist(
                        operationalClose,
                        operationalEvent,
                        REVISED_AT,
                        ACTOR))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "immutable");

        verifyNoInteractions(
                dependentResultInvalidator,
                eventRevisionRepository,
                closeRevisionRepository,
                uuidGenerator);
    }

    private static OperationalClose closeWithState(
            OperationalCloseState state) {

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
                state,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static OperationalEvent eventWithState(
            OperationalEventState state) {

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
                state,
                false,
                false,
                1L,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

}