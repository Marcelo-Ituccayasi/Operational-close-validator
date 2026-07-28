package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationContext;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationEngine;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationRuleEvaluation;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationStateResolver;
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
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

class ValidateOperationalEventTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "26a86d26-837b-42b8-8117-200000000001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "26a86d26-837b-42b8-8117-200000000002");

    private static final UUID PREVIOUS_RESULT_UUID =
            UUID.fromString(
                    "26a86d26-837b-42b8-8117-200000000003");

    private static final UUID CURRENT_RESULT_UUID =
            UUID.fromString(
                    "26a86d26-837b-42b8-8117-200000000004");

    private static final UUID TRANSITION_UUID =
            UUID.fromString(
                    "26a86d26-837b-42b8-8117-200000000005");

    private static final OperationalCloseId CLOSE_ID =
            new OperationalCloseId(
                    CLOSE_UUID);

    private static final OperationalEventId EVENT_ID =
            new OperationalEventId(
                    EVENT_UUID);

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-28T16:00:00Z");

    private static final Instant PREVIOUS_EVALUATION_AT =
            Instant.parse(
                    "2026-07-28T16:30:00Z");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-28T17:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    AuditActor.RESPONSIBLE_USER_ID,
                    "responsible");

    private final OperationalCloseLockRepository
            closeLockRepository =
                    mock(
                            OperationalCloseLockRepository.class);

    private final OperationalEventRevisionRepository
            eventRevisionRepository =
                    mock(
                            OperationalEventRevisionRepository.class);

    private final EventValidationResultRepository
            validationResultRepository =
                    mock(
                            EventValidationResultRepository.class);

    private final CurrentActorProvider
            currentActorProvider =
                    mock(
                            CurrentActorProvider.class);

    private final ApplicationClock applicationClock =
            mock(
                    ApplicationClock.class);

    private final UuidGenerator uuidGenerator =
            mock(
                    UuidGenerator.class);

    private final EventValidationContextLoader contextLoader =
            mock(
                    EventValidationContextLoader.class);

    private final EventValidationEngine validationEngine =
            mock(
                    EventValidationEngine.class);

    private final EventValidationResultFactory resultFactory =
            mock(
                    EventValidationResultFactory.class);

    private final EventValidationAlertSynchronizer
            alertSynchronizer =
                    mock(
                            EventValidationAlertSynchronizer.class);

    private final EventValidationStateResolver stateResolver =
            mock(
                    EventValidationStateResolver.class);

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

    private final ValidateOperationalEvent useCase =
            new ValidateOperationalEvent(
                    closeLockRepository,
                    eventRevisionRepository,
                    validationResultRepository,
                    currentActorProvider,
                    applicationClock,
                    uuidGenerator,
                    transactionRunner,
                    contextLoader,
                    validationEngine,
                    resultFactory,
                    alertSynchronizer,
                    stateResolver);

    @Test
    void validatesEventAfterLockingScopeAndPersistsAtomicChanges() {
        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        EventValidationContext context =
                context(
                        operationalEvent);

        EventValidationRuleEvaluation evaluation =
                satisfiedEvaluation();

        EventValidationResult previousResult =
                previousFailedResult();

        EventValidationResult currentResult =
                currentSatisfiedResult();

        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent));

        when(
                contextLoader.load(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                context));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);

        when(
                validationEngine.evaluate(
                        any(
                                EventValidationContext.class)))
                .thenReturn(
                        List.of(
                                evaluation));

        when(
                validationResultRepository
                        .findAllCurrentByEventIdOrderByRuleCode(
                                EVENT_ID))
                .thenReturn(
                        List.of(
                                previousResult));

        when(
                resultFactory.createAll(
                        any(
                                EventValidationContext.class),
                        any(),
                        any(),
                        any()))
                .thenReturn(
                        List.of(
                                currentResult));

        when(
                stateResolver.resolve(
                        List.of(
                                evaluation)))
                .thenReturn(
                        OperationalEventState.VALIDATED);

        when(
                uuidGenerator.next())
                .thenReturn(
                        TRANSITION_UUID);

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .VALIDATED);

        assertThat(
                result.eventId())
                .isEqualTo(
                        EVENT_UUID);

        assertThat(
                result.message())
                .isNull();

        InOrder lockOrder =
                inOrder(
                        closeLockRepository,
                        eventRevisionRepository);

        lockOrder.verify(
                closeLockRepository)
                .findByIdForUpdate(
                        CLOSE_ID);

        lockOrder.verify(
                eventRevisionRepository)
                .findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID);

        ArgumentCaptor<EventValidationResult>
                invalidatedResultCaptor =
                        ArgumentCaptor.forClass(
                                EventValidationResult.class);

        verify(
                validationResultRepository)
                .saveInvalidation(
                        invalidatedResultCaptor.capture());

        EventValidationResult invalidatedResult =
                invalidatedResultCaptor.getValue();

        assertThat(
                invalidatedResult.current())
                .isFalse();

        assertThat(
                invalidatedResult.invalidatedAt())
                .isEqualTo(
                        NOW);

        assertThat(
                invalidatedResult.invalidationReason())
                .isEqualTo(
                        "Operational Event was revalidated.");

        verify(
                validationResultRepository)
                .saveNew(
                        currentResult);

        verify(
                alertSynchronizer)
                .synchronize(
                        List.of(
                                currentResult),
                        NOW,
                        ACTOR);

        ArgumentCaptor<OperationalEvent>
                revisedEventCaptor =
                        ArgumentCaptor.forClass(
                                OperationalEvent.class);

        verify(
                eventRevisionRepository)
                .saveRevision(
                        revisedEventCaptor.capture());

        assertThat(
                revisedEventCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(
                revisedEventCaptor.getValue()
                        .dataRevision())
                .isEqualTo(
                        operationalEvent.dataRevision());

        ArgumentCaptor<EventStateTransition>
                transitionCaptor =
                        ArgumentCaptor.forClass(
                                EventStateTransition.class);

        verify(
                eventRevisionRepository)
                .appendStateTransition(
                        transitionCaptor.capture());

        EventStateTransition transition =
                transitionCaptor.getValue();

        assertThat(
                transition.id())
                .isEqualTo(
                        new EventStateTransitionId(
                                TRANSITION_UUID));

        assertThat(
                transition.fromState())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(
                transition.toState())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(
                transition.occurredAt())
                .isEqualTo(
                        NOW);
    }

    @Test
    void persistsFailedValidationAndDerivesPendingSupport() {
        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        EventValidationRuleEvaluation evaluation =
                failedEvaluation();

        EventValidationResult failedResult =
                currentFailedResult();

        stubExecutableScope(
                operationalEvent);

        when(
                validationEngine.evaluate(
                        any(
                                EventValidationContext.class)))
                .thenReturn(
                        List.of(
                                evaluation));

        when(
                validationResultRepository
                        .findAllCurrentByEventIdOrderByRuleCode(
                                EVENT_ID))
                .thenReturn(
                        List.of());

        when(
                resultFactory.createAll(
                        any(
                                EventValidationContext.class),
                        any(),
                        any(),
                        any()))
                .thenReturn(
                        List.of(
                                failedResult));

        when(
                stateResolver.resolve(
                        List.of(
                                evaluation)))
                .thenReturn(
                        OperationalEventState
                                .PENDING_SUPPORT);

        when(
                uuidGenerator.next())
                .thenReturn(
                        TRANSITION_UUID);

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .VALIDATION_FAILED);

        assertThat(
                result.eventId())
                .isEqualTo(
                        EVENT_UUID);

        assertThat(
                result.message())
                .isEqualTo(
                        "The event contains failed validation rules.");

        verify(
                validationResultRepository)
                .saveNew(
                        failedResult);

        verify(
                alertSynchronizer)
                .synchronize(
                        List.of(
                                failedResult),
                        NOW,
                        ACTOR);

        ArgumentCaptor<OperationalEvent>
                eventCaptor =
                        ArgumentCaptor.forClass(
                                OperationalEvent.class);

        verify(
                eventRevisionRepository)
                .saveRevision(
                        eventCaptor.capture());

        assertThat(
                eventCaptor.getValue()
                        .state())
                .isEqualTo(
                        OperationalEventState
                                .PENDING_SUPPORT);
    }

    @Test
    void revalidatesWithoutWritingStateWhenStateDoesNotChange() {
        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.VALIDATED);

        EventValidationRuleEvaluation evaluation =
                satisfiedEvaluation();

        EventValidationResult currentResult =
                currentSatisfiedResult();

        stubExecutableScope(
                operationalEvent);

        when(
                validationEngine.evaluate(
                        any(
                                EventValidationContext.class)))
                .thenReturn(
                        List.of(
                                evaluation));

        when(
                validationResultRepository
                        .findAllCurrentByEventIdOrderByRuleCode(
                                EVENT_ID))
                .thenReturn(
                        List.of());

        when(
                resultFactory.createAll(
                        any(
                                EventValidationContext.class),
                        any(),
                        any(),
                        any()))
                .thenReturn(
                        List.of(
                                currentResult));

        when(
                stateResolver.resolve(
                        List.of(
                                evaluation)))
                .thenReturn(
                        OperationalEventState.VALIDATED);

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .VALIDATED);

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
                uuidGenerator);
    }

    @Test
    void rejectsInvalidIdentifiersBeforeAcquiringLocks() {
        stubResponsibleActor();

        ValidateOperationalEventResult result =
                useCase.execute(
                        new ValidateOperationalEventCommand(
                                CLOSE_UUID,
                                null));

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .INVALID_INPUT);

        assertThat(
                result.message())
                .isEqualTo(
                        "eventId must not be null");

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    @Test
    void rejectsUnauthorizedActorBeforeAcquiringLocks() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                "other-role",
                                "other-user"));

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .ACTOR_REJECTED);

        verifyNoInteractions(
                closeLockRepository,
                eventRevisionRepository,
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    @Test
    void returnsCloseNotFoundWithoutLockingEvent() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.empty());

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .CLOSE_NOT_FOUND);

        verifyNoInteractions(
                eventRevisionRepository,
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    @Test
    void rejectsSentCloseWithoutLockingEvent() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState
                                                .SENT_TO_ACCOUNTING)));

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .CLOSE_NOT_EDITABLE);

        verifyNoInteractions(
                eventRevisionRepository,
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    @Test
    void returnsEventNotFoundWithoutLoadingValidationContext() {
        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.empty());

        ValidateOperationalEventResult result =
                useCase.execute(
                        validCommand());

        assertThat(
                result.status())
                .isEqualTo(
                        ValidateOperationalEventResult.Status
                                .EVENT_NOT_FOUND);

        verifyNoInteractions(
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    @Test
    void rejectsMissingContextAfterEventWasLocked() {
        OperationalEvent operationalEvent =
                eventWithState(
                        OperationalEventState.REGISTERED);

        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent));

        when(
                contextLoader.load(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.empty());

        assertThatThrownBy(
                () -> useCase.execute(
                        validCommand()))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "locked operational event could not be loaded for validation");

        verifyNoInteractions(
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    @Test
    void rejectsNullCommandBeforeStartingBusinessProcessing() {
        assertThatThrownBy(
                () -> useCase.execute(
                        null))
                .isInstanceOf(
                        NullPointerException.class)
                .hasMessage(
                        "command must not be null");

        verifyNoInteractions(
                currentActorProvider,
                closeLockRepository,
                eventRevisionRepository,
                validationResultRepository,
                applicationClock,
                uuidGenerator,
                contextLoader,
                validationEngine,
                resultFactory,
                alertSynchronizer,
                stateResolver);
    }

    private void stubExecutableScope(
            OperationalEvent operationalEvent) {

        stubResponsibleActor();

        when(
                closeLockRepository.findByIdForUpdate(
                        CLOSE_ID))
                .thenReturn(
                        Optional.of(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)));

        when(
                eventRevisionRepository.findByIdForUpdate(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                operationalEvent));

        when(
                contextLoader.load(
                        CLOSE_ID,
                        EVENT_ID))
                .thenReturn(
                        Optional.of(
                                context(
                                        operationalEvent)));

        when(
                applicationClock.now())
                .thenReturn(
                        NOW);
    }

    private void stubResponsibleActor() {
        when(
                currentActorProvider.currentActor())
                .thenReturn(
                        new AuthenticatedPrincipal(
                                AuditActor.RESPONSIBLE_USER_ID,
                                "responsible"));
    }

    private static ValidateOperationalEventCommand
            validCommand() {

        return new ValidateOperationalEventCommand(
                CLOSE_UUID,
                EVENT_UUID);
    }

    private static EventValidationContext context(
            OperationalEvent operationalEvent) {

        return new EventValidationContext(
                operationalEvent,
                List.of(),
                List.of());
    }

    private static EventValidationRuleEvaluation
            satisfiedEvaluation() {

        return new EventValidationRuleEvaluation(
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                ValidationOutcome.SATISFIED,
                "VR-003 satisfied.");
    }

    private static EventValidationRuleEvaluation
            failedEvaluation() {

        return new EventValidationRuleEvaluation(
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                ValidationOutcome.FAILED,
                "VR-003 failed.");
    }

    private static EventValidationResult previousFailedResult() {
        return EventValidationResult.create(
                new ValidationResultId(
                        PREVIOUS_RESULT_UUID),
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                EVENT_ID,
                ValidationOutcome.FAILED,
                "Previous VR-003 failure.",
                PREVIOUS_EVALUATION_AT,
                ACTOR,
                1L);
    }

    private static EventValidationResult currentSatisfiedResult() {
        return EventValidationResult.create(
                new ValidationResultId(
                        CURRENT_RESULT_UUID),
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                EVENT_ID,
                ValidationOutcome.SATISFIED,
                "VR-003 satisfied.",
                NOW,
                ACTOR,
                1L);
    }

    private static EventValidationResult currentFailedResult() {
        return EventValidationResult.create(
                new ValidationResultId(
                        CURRENT_RESULT_UUID),
                ValidationRuleCode.VR_003,
                EventValidationEngine.MVP_RULE_VERSION,
                EVENT_ID,
                ValidationOutcome.FAILED,
                "VR-003 failed.",
                NOW,
                ACTOR,
                1L);
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
                        60L),
                CREATED_AT,
                "Caja principal",
                "Gasto operativo para validación",
                state,
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