package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
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

class CompleteOperationalCloseConsolidationTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "6bc97019-7c04-41ba-a982-770000000001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "6bc97019-7c04-41ba-a982-770000000002");

    private static final UUID CONSOLIDATION_UUID =
            UUID.fromString(
                    "6bc97019-7c04-41ba-a982-770000000003");

    private static final UUID PREVIOUS_CONSOLIDATION_UUID =
            UUID.fromString(
                    "6bc97019-7c04-41ba-a982-770000000004");

    private static final UUID TRANSITION_UUID =
            UUID.fromString(
                    "6bc97019-7c04-41ba-a982-770000000005");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final ConsolidationId CONSOLIDATION_ID =
            new ConsolidationId(
                    CONSOLIDATION_UUID);

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-30T12:00:00Z");

    private static final Instant PREVIOUS_COMPLETED_AT =
            Instant.parse(
                    "2026-07-30T12:30:00Z");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-30T13:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalCloseLockRepository
            closeLockRepository =
                    mock(
                            OperationalCloseLockRepository.class);

    private final OperationalCloseRevisionRepository
            closeRevisionRepository =
                    mock(
                            OperationalCloseRevisionRepository.class);

    private final OperationalEventRepository eventRepository =
            mock(
                    OperationalEventRepository.class);

    private final ConsolidationRepository consolidationRepository =
            mock(
                    ConsolidationRepository.class);

    private final CloseConsolidationReadinessEvaluator
            readinessEvaluator =
                    mock(
                            CloseConsolidationReadinessEvaluator.class);

    private final CurrentActorProvider currentActorProvider =
            mock(
                    CurrentActorProvider.class);

    private final ApplicationClock applicationClock =
            mock(
                    ApplicationClock.class);

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

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

    private final CompleteOperationalCloseConsolidation useCase =
            new CompleteOperationalCloseConsolidation(
                    closeLockRepository,
                    closeRevisionRepository,
                    eventRepository,
                    consolidationRepository,
                    readinessEvaluator,
                    currentActorProvider,
                    applicationClock,
                    uuidGenerator,
                    transactionRunner);

    @Test
    void completesConsolidationAndValidatesTheClose() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.PREPARATION);

        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED);

        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(eventRepository
                .findAllByCloseIdOrderByOccurredAtDescending(
                        CLOSE_ID))
                .thenReturn(
                        List.of(
                                event));

        when(readinessEvaluator.evaluate(
                CLOSE_ID,
                List.of(
                        event)))
                .thenReturn(
                        ready());

        when(applicationClock.now())
                .thenReturn(
                        NOW);

        when(uuidGenerator.next())
                .thenReturn(
                        CONSOLIDATION_UUID,
                        TRANSITION_UUID);

        when(consolidationRepository
                .findCurrentByCloseId(
                        CLOSE_ID))
                .thenReturn(
                        Optional.empty());

        CompleteOperationalCloseConsolidationResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        CompleteOperationalCloseConsolidationResult
                                .Status.CONSOLIDATED);

        assertThat(
                result.consolidationId())
                .isEqualTo(
                        CONSOLIDATION_UUID);

        assertThat(
                result.affectedEventIds())
                .isEmpty();

        assertThat(
                result.message())
                .isNull();

        ArgumentCaptor<Consolidation> consolidationCaptor =
                ArgumentCaptor.forClass(
                        Consolidation.class);

        verify(consolidationRepository)
                .saveNew(
                        consolidationCaptor.capture());

        Consolidation savedConsolidation =
                consolidationCaptor.getValue();

        assertThat(
                savedConsolidation.id())
                .isEqualTo(
                        CONSOLIDATION_ID);

        assertThat(
                savedConsolidation.closeId())
                .isEqualTo(
                        CLOSE_ID);

        assertThat(
                savedConsolidation.eventCount())
                .isEqualTo(
                        1);

        assertThat(
                savedConsolidation.expectedBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1125.0000"));

        assertThat(
                savedConsolidation.actualBalance())
                .isEqualByComparingTo(
                        decimal(
                                "1125.0000"));

        assertThat(
                savedConsolidation.difference())
                .isEqualByComparingTo(
                        BigDecimal.ZERO);

        assertThat(
                savedConsolidation.current())
                .isTrue();

        ArgumentCaptor<OperationalClose> closeCaptor =
                ArgumentCaptor.forClass(
                        OperationalClose.class);

        verify(closeRevisionRepository)
                .saveRevision(
                        closeCaptor.capture());

        OperationalClose savedClose =
                closeCaptor.getValue();

        assertThat(
                savedClose.state())
                .isEqualTo(
                        OperationalCloseState.VALIDATED);

        assertThat(
                savedClose.stateChangedAt())
                .isEqualTo(
                        NOW);

        assertThat(
                savedClose.updatedAt())
                .isEqualTo(
                        NOW);

        assertThat(
                savedClose.updatedBy())
                .isEqualTo(
                        ACTOR);

        ArgumentCaptor<CloseStateTransition> transitionCaptor =
                ArgumentCaptor.forClass(
                        CloseStateTransition.class);

        ArgumentCaptor<ConsolidationId>
                transitionConsolidationIdCaptor =
                        ArgumentCaptor.forClass(
                                ConsolidationId.class);

        verify(closeRevisionRepository)
                .appendConsolidationStateTransition(
                        transitionCaptor.capture(),
                        transitionConsolidationIdCaptor.capture());

        CloseStateTransition transition =
                transitionCaptor.getValue();

        assertThat(
                transition.fromState())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(
                transition.toState())
                .isEqualTo(
                        OperationalCloseState.VALIDATED);

        assertThat(
                transition.causeCode())
                .isEqualTo(
                        CompleteOperationalCloseConsolidation
                                .CONSOLIDATION_COMPLETED);

        assertThat(
                transition.occurredAt())
                .isEqualTo(
                        NOW);

        assertThat(
                transition.actor())
                .isEqualTo(
                        ACTOR);

        assertThat(
                transitionConsolidationIdCaptor.getValue())
                .isEqualTo(
                        CONSOLIDATION_ID);

        verify(consolidationRepository, never())
                .saveInvalidation(
                        any(
                                Consolidation.class));
    }

    @Test
    void invalidatesPreviousConsolidationBeforeSavingReplacement() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.BLOCKED);

        OperationalEvent event =
                event(
                        OperationalEventState.VALIDATED);

        Consolidation previous =
                previousConsolidation(
                        operationalClose,
                        event);

        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(eventRepository
                .findAllByCloseIdOrderByOccurredAtDescending(
                        CLOSE_ID))
                .thenReturn(
                        List.of(
                                event));

        when(readinessEvaluator.evaluate(
                CLOSE_ID,
                List.of(
                        event)))
                .thenReturn(
                        ready());

        when(applicationClock.now())
                .thenReturn(
                        NOW);

        when(uuidGenerator.next())
                .thenReturn(
                        CONSOLIDATION_UUID,
                        TRANSITION_UUID);

        when(consolidationRepository
                .findCurrentByCloseId(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                previous));

        useCase.execute(
                command());

        ArgumentCaptor<Consolidation> invalidatedCaptor =
                ArgumentCaptor.forClass(
                        Consolidation.class);

        InOrder persistenceOrder =
                inOrder(
                        consolidationRepository);

        persistenceOrder.verify(
                consolidationRepository)
                .findCurrentByCloseId(
                        CLOSE_ID);

        persistenceOrder.verify(
                consolidationRepository)
                .saveInvalidation(
                        invalidatedCaptor.capture());

        persistenceOrder.verify(
                consolidationRepository)
                .saveNew(
                        any(
                                Consolidation.class));

        Consolidation invalidated =
                invalidatedCaptor.getValue();

        assertThat(
                invalidated.id())
                .isEqualTo(
                        previous.id());

        assertThat(
                invalidated.current())
                .isFalse();

        assertThat(
                invalidated.invalidatedAt())
                .isEqualTo(
                        NOW);

        assertThat(
                invalidated.invalidationReason())
                .isEqualTo(
                        "A newer consolidation was completed.");

        assertThat(
                invalidated.eventSnapshots())
                .isEqualTo(
                        previous.eventSnapshots());
    }

    @Test
    void rejectsNotReadyCloseAndMovesPreparationToBlocked() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.PREPARATION);

        OperationalEvent event =
                event(
                        OperationalEventState.OBSERVED);

        CloseConsolidationReadiness readiness =
                CloseConsolidationReadiness.evaluated(
                        List.of(
                                EVENT_ID),
                        List.of(
                                EVENT_ID),
                        List.of());

        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(eventRepository
                .findAllByCloseIdOrderByOccurredAtDescending(
                        CLOSE_ID))
                .thenReturn(
                        List.of(
                                event));

        when(readinessEvaluator.evaluate(
                CLOSE_ID,
                List.of(
                        event)))
                .thenReturn(
                        readiness);

        when(applicationClock.now())
                .thenReturn(
                        NOW);

        when(uuidGenerator.next())
                .thenReturn(
                        TRANSITION_UUID);

        CompleteOperationalCloseConsolidationResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        CompleteOperationalCloseConsolidationResult
                                .Status.CONSOLIDATION_REJECTED);

        assertThat(
                result.consolidationId())
                .isNull();

        assertThat(
                result.affectedEventIds())
                .containsExactly(
                        EVENT_UUID);

        assertThat(
                result.message())
                .contains(
                        "require correction");

        ArgumentCaptor<OperationalClose> closeCaptor =
                ArgumentCaptor.forClass(
                        OperationalClose.class);

        verify(closeRevisionRepository)
                .saveRevision(
                        closeCaptor.capture());

        assertThat(
                closeCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        ArgumentCaptor<CloseStateTransition> transitionCaptor =
                ArgumentCaptor.forClass(
                        CloseStateTransition.class);

        verify(closeRevisionRepository)
                .appendStateTransition(
                        transitionCaptor.capture());

        CloseStateTransition transition =
                transitionCaptor.getValue();

        assertThat(
                transition.fromState())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(
                transition.toState())
                .isEqualTo(
                        OperationalCloseState.BLOCKED);

        assertThat(
                transition.causeCode())
                .isEqualTo(
                        CompleteOperationalCloseConsolidation
                                .CONSOLIDATION_REJECTED);

        verifyNoInteractions(
                consolidationRepository);
    }

    @Test
    void keepsAlreadyBlockedCloseWithoutDuplicateTransition() {
        OperationalClose operationalClose =
                close(
                        OperationalCloseState.BLOCKED);

        OperationalEvent event =
                event(
                        OperationalEventState.OBSERVED);

        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                operationalClose));

        when(eventRepository
                .findAllByCloseIdOrderByOccurredAtDescending(
                        CLOSE_ID))
                .thenReturn(
                        List.of(
                                event));

        when(readinessEvaluator.evaluate(
                CLOSE_ID,
                List.of(
                        event)))
                .thenReturn(
                        CloseConsolidationReadiness.evaluated(
                                List.of(
                                        EVENT_ID),
                                List.of(),
                                List.of()));

        when(applicationClock.now())
                .thenReturn(
                        NOW);

        CompleteOperationalCloseConsolidationResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        CompleteOperationalCloseConsolidationResult
                                .Status.CONSOLIDATION_REJECTED);

        verifyNoInteractions(
                closeRevisionRepository,
                consolidationRepository,
                uuidGenerator);
    }

    @Test
    void rejectsInvalidActualBalanceBeforeLockingClose() {
        configureResponsibleActor();

        CompleteOperationalCloseConsolidationResult result =
                useCase.execute(
                        new CompleteOperationalCloseConsolidationCommand(
                                CLOSE_UUID,
                                new BigDecimal(
                                        "1125.00001")));

        assertThat(
                result.status())
                .isEqualTo(
                        CompleteOperationalCloseConsolidationResult
                                .Status.INVALID_INPUT);

        assertThat(
                result.message())
                .isEqualTo(
                        "actual balance must not exceed "
                                + "four decimal places");

        verifyNoInteractions(
                closeLockRepository,
                closeRevisionRepository,
                eventRepository,
                consolidationRepository,
                readinessEvaluator,
                applicationClock,
                uuidGenerator);
    }

    @Test
    void rejectsActorBeforeLockingClose() {
        when(currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                "other-user",
                                "other"));

        CompleteOperationalCloseConsolidationResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        CompleteOperationalCloseConsolidationResult
                                .Status.ACTOR_REJECTED);

        verifyNoInteractions(
                closeLockRepository,
                closeRevisionRepository,
                eventRepository,
                consolidationRepository,
                readinessEvaluator,
                applicationClock,
                uuidGenerator);
    }

    @Test
    void rejectsCloseOutsidePreparationOrBlockedState() {
        configureResponsibleActor();

        when(closeLockRepository.findByIdForUpdate(
                CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                close(
                                        OperationalCloseState.VALIDATED)));

        CompleteOperationalCloseConsolidationResult result =
                useCase.execute(
                        command());

        assertThat(
                result.status())
                .isEqualTo(
                        CompleteOperationalCloseConsolidationResult
                                .Status.CLOSE_NOT_CONSOLIDATABLE);

        verifyNoInteractions(
                closeRevisionRepository,
                eventRepository,
                consolidationRepository,
                readinessEvaluator,
                applicationClock,
                uuidGenerator);
    }

    private void configureResponsibleActor() {
        when(currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                AuditActor.RESPONSIBLE_USER_ID,
                                "responsible"));
    }

    private CompleteOperationalCloseConsolidationCommand command() {
        return new CompleteOperationalCloseConsolidationCommand(
                CLOSE_UUID,
                decimal(
                        "1125.0000"));
    }

    private CloseConsolidationReadiness ready() {
        return CloseConsolidationReadiness.evaluated(
                List.of(),
                List.of(),
                List.of());
    }

    private Consolidation previousConsolidation(
            OperationalClose operationalClose,
            OperationalEvent event) {

        return Consolidation.complete(
                new ConsolidationId(
                        PREVIOUS_CONSOLIDATION_UUID),
                operationalClose,
                List.of(
                        event),
                decimal(
                        "1125.0000"),
                PREVIOUS_COMPLETED_AT,
                ACTOR);
    }

    private OperationalClose close(
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
                        decimal(
                                "1000.0000")),
                state,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private OperationalEvent event(
            OperationalEventState state) {

        return new OperationalEvent(
                EVENT_ID,
                CLOSE_ID,
                OperationalEventType.INCOME,
                new OperationalEventAmount(
                        decimal(
                                "125.0000")),
                decimal(
                        "125.0000"),
                null,
                CREATED_AT,
                CREATED_AT,
                "Caja principal",
                "Evento utilizado para consolidación",
                state,
                false,
                false,
                4,
                CREATED_AT,
                CREATED_AT,
                ACTOR,
                CREATED_AT,
                ACTOR);
    }

    private static BigDecimal decimal(
            String value) {

        return new BigDecimal(
                value);
    }

}