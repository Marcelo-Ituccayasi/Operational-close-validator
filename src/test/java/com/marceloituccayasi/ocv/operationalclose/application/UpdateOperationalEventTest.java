package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventRequirementPolicy;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class UpdateOperationalEventTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "66332f33-b75a-42f6-b784-5ff167100001");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "66332f33-b75a-42f6-b784-5ff167100002");

    private static final UUID CANCELLATION_ID =
            UUID.fromString(
                    "66332f33-b75a-42f6-b784-5ff167100003");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "66332f33-b75a-42f6-b784-5ff167100004");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-22T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-22T10:00:00Z");

    @Test
    void revisesRegularEventUnderCloseAndEventLocks() {
        List<String> operations =
                new ArrayList<>();

        OperationalEvent originalEvent =
                regularEvent(
                        EVENT_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        eventRepository.events.put(
                originalEvent.id(),
                originalEvent);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        operationalClose());

        UpdateOperationalEventResult result =
                useCase.execute(
                        updateCommand(
                                "EXPENSE",
                                "95.5000"));

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status.UPDATED);

        assertThat(result.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(eventRepository.savedEvents)
                .hasSize(1);

        OperationalEvent revisedEvent =
                eventRepository.savedEvents.getFirst();

        assertThat(revisedEvent.id())
                .isEqualTo(originalEvent.id());

        assertThat(revisedEvent.eventType())
                .isEqualTo(
                        OperationalEventType.EXPENSE);

        assertThat(revisedEvent.amount().value())
                .isEqualByComparingTo("95.5000");

        assertThat(revisedEvent.balanceEffect())
                .isEqualByComparingTo("-95.5000");

        assertThat(revisedEvent.responsibleName())
                .isEqualTo("Caja revisada");

        assertThat(revisedEvent.description())
                .isEqualTo("Evento operativo revisado");

        assertThat(revisedEvent.dataRevision())
                .isEqualTo(2L);

        assertThat(revisedEvent.updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(revisedEvent.updatedBy())
                .isEqualTo(actor());

        assertThat(eventRepository.transitions)
                .isEmpty();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID,
                        "lock-cancellation:" + EVENT_ID,
                        "save:" + EVENT_ID);
    }

    @Test
    void recalculatesDependentCancellationWhenOriginalChanges() {
        List<String> operations =
                new ArrayList<>();

        OperationalEvent originalEvent =
                regularEvent(
                        EVENT_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        OperationalEvent cancellation =
                OperationalEvent.createCancellation(
                        new OperationalEventId(
                                CANCELLATION_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        originalEvent,
                        CREATED_AT.plusSeconds(60),
                        "Caja principal",
                        "Anulación inicial",
                        false,
                        false,
                        CREATED_AT.plusSeconds(120),
                        actor());

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        eventRepository.events.put(
                originalEvent.id(),
                originalEvent);

        eventRepository.cancellations.put(
                originalEvent.id(),
                cancellation);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        operationalClose());

        UpdateOperationalEventResult result =
                useCase.execute(
                        updateCommand(
                                "EXPENSE",
                                "125.2500"));

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status.UPDATED);

        assertThat(eventRepository.savedEvents)
                .hasSize(2);

        OperationalEvent revisedOriginal =
                eventRepository.savedEvents.get(0);

        OperationalEvent revisedCancellation =
                eventRepository.savedEvents.get(1);

        assertThat(revisedOriginal.id().value())
                .isEqualTo(EVENT_ID);

        assertThat(revisedOriginal.amount().value())
                .isEqualByComparingTo("125.2500");

        assertThat(revisedOriginal.balanceEffect())
                .isEqualByComparingTo("-125.2500");

        assertThat(revisedOriginal.dataRevision())
                .isEqualTo(2L);

        assertThat(revisedCancellation.id().value())
                .isEqualTo(CANCELLATION_ID);

        assertThat(revisedCancellation.reversedEventId())
                .isEqualTo(revisedOriginal.id());

        assertThat(revisedCancellation.amount().value())
                .isEqualByComparingTo("125.2500");

        assertThat(revisedCancellation.balanceEffect())
                .isEqualByComparingTo("125.2500");

        assertThat(revisedCancellation.dataRevision())
                .isEqualTo(2L);

        assertThat(revisedCancellation.updatedAt())
                .isEqualTo(REVISED_AT);

        assertThat(eventRepository.transitions)
                .isEmpty();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID,
                        "lock-cancellation:" + EVENT_ID,
                        "save:" + EVENT_ID,
                        "save:" + CANCELLATION_ID);
    }

    @Test
    void returnsValidatedEventToRegisteredAndAppendsTransition() {
        List<String> operations =
                new ArrayList<>();

        OperationalEvent registeredEvent =
                regularEvent(
                        EVENT_ID,
                        OperationalEventType.DISCOUNT,
                        "25.0000");

        OperationalEvent validatedEvent =
                withState(
                        registeredEvent,
                        OperationalEventState.VALIDATED);

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        eventRepository.events.put(
                validatedEvent.id(),
                validatedEvent);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        operationalClose());

        UpdateOperationalEventResult result =
                useCase.execute(
                        updateCommand(
                                "DISCOUNT",
                                "30.0000"));

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status.UPDATED);

        assertThat(eventRepository.savedEvents)
                .hasSize(1);

        OperationalEvent revisedEvent =
                eventRepository.savedEvents.getFirst();

        assertThat(revisedEvent.state())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(revisedEvent.stateChangedAt())
                .isEqualTo(REVISED_AT);

        assertThat(revisedEvent.dataRevision())
                .isEqualTo(2L);

        assertThat(eventRepository.transitions)
                .hasSize(1);

        EventStateTransition transition =
                eventRepository.transitions.getFirst();

        assertThat(transition.id().value())
                .isEqualTo(TRANSITION_ID);

        assertThat(transition.eventId().value())
                .isEqualTo(EVENT_ID);

        assertThat(transition.fromState())
                .isEqualTo(
                        OperationalEventState.VALIDATED);

        assertThat(transition.toState())
                .isEqualTo(
                        OperationalEventState.REGISTERED);

        assertThat(transition.causeCode())
                .isEqualTo(
                        "EVENT_DATA_REVISED");

        assertThat(transition.occurredAt())
                .isEqualTo(REVISED_AT);

        assertThat(transition.actor())
                .isEqualTo(actor());

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID,
                        "lock-cancellation:" + EVENT_ID,
                        "save:" + EVENT_ID,
                        "append-transition:" + TRANSITION_ID);
    }

    private static UpdateOperationalEvent newUseCase(
            List<String> operations,
            StubRevisionStore eventRepository,
            OperationalClose operationalClose) {

        OperationalCloseLockRepository closeRepository =
                closeId -> {
                    operations.add(
                            "lock-close");

                    if (operationalClose == null
                            || !operationalClose.id()
                                    .equals(closeId)) {

                        return Optional.empty();
                    }

                    return Optional.of(
                            operationalClose);
                };

        CurrentActorProvider currentActorProvider =
                () -> new AuthenticatedPrincipal(
                        AuditActor.RESPONSIBLE_USER_ID,
                        "responsible");

        ApplicationClock applicationClock =
                () -> REVISED_AT;

        UuidGenerator uuidGenerator =
                () -> TRANSITION_ID;

        TransactionRunner transactionRunner =
                new DirectTransactionRunner(
                        operations);

        OperationalEventRequirementPolicy requirementPolicy =
                (eventType, amount) ->
                        new OperationalEventRequirements(
                                false,
                                false);

        return new UpdateOperationalEvent(
                closeRepository,
                eventRepository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner,
                requirementPolicy);
    }

    private static UpdateOperationalEventCommand updateCommand(
            String eventType,
            String amount) {

        return new UpdateOperationalEventCommand(
                CLOSE_ID,
                EVENT_ID,
                eventType,
                new BigDecimal(amount),
                null,
                REVISED_AT.minusSeconds(300),
                "Caja revisada",
                "Evento operativo revisado");
    }

    private static OperationalClose operationalClose() {
        return OperationalClose.create(
                new OperationalCloseId(
                        CLOSE_ID),
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("1000.0000")),
                CREATED_AT,
                actor());
    }

    private static OperationalEvent regularEvent(
            UUID eventId,
            OperationalEventType eventType,
            String amount) {

        return OperationalEvent.create(
                new OperationalEventId(
                        eventId),
                new OperationalCloseId(
                        CLOSE_ID),
                eventType,
                new OperationalEventAmount(
                        new BigDecimal(amount)),
                CREATED_AT.plusSeconds(30),
                "Caja principal",
                "Evento operativo inicial",
                false,
                false,
                CREATED_AT.plusSeconds(60),
                actor());
    }

    private static OperationalEvent withState(
            OperationalEvent operationalEvent,
            OperationalEventState state) {

        return new OperationalEvent(
                operationalEvent.id(),
                operationalEvent.closeId(),
                operationalEvent.eventType(),
                operationalEvent.amount(),
                operationalEvent.balanceEffect(),
                operationalEvent.reversedEventId(),
                operationalEvent.occurredAt(),
                operationalEvent.registeredAt(),
                operationalEvent.responsibleName(),
                operationalEvent.description(),
                state,
                operationalEvent.evidenceRequired(),
                operationalEvent.authorizationRequired(),
                operationalEvent.dataRevision(),
                CREATED_AT.plusSeconds(180),
                operationalEvent.createdAt(),
                operationalEvent.createdBy(),
                CREATED_AT.plusSeconds(180),
                operationalEvent.updatedBy());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

    private static final class DirectTransactionRunner
            implements TransactionRunner {

        private final List<String> operations;

        private DirectTransactionRunner(
                List<String> operations) {

            this.operations =
                    operations;
        }

        @Override
        public <T> T execute(
                Supplier<T> operation) {

            operations.add(
                    "transaction");

            return operation.get();
        }

    }

    private static final class StubRevisionStore
            implements OperationalEventRevisionRepository {

        private final Map<OperationalEventId, OperationalEvent>
                events =
                        new HashMap<>();

        private final Map<OperationalEventId, OperationalEvent>
                cancellations =
                        new HashMap<>();

        private final List<OperationalEvent>
                savedEvents =
                        new ArrayList<>();

        private final List<EventStateTransition>
                transitions =
                        new ArrayList<>();

        private final List<String> operations;

        private StubRevisionStore(
                List<String> operations) {

            this.operations =
                    operations;
        }

        @Override
        public Optional<OperationalEvent> findByIdForUpdate(
                OperationalCloseId closeId,
                OperationalEventId eventId) {

            operations.add(
                    "lock-event:"
                            + eventId.value());

            return Optional.ofNullable(
                    events.get(eventId));
        }

        @Override
        public Optional<OperationalEvent>
                findCancellationByReversedEventIdForUpdate(
                        OperationalCloseId closeId,
                        OperationalEventId reversedEventId) {

            operations.add(
                    "lock-cancellation:"
                            + reversedEventId.value());

            return Optional.ofNullable(
                    cancellations.get(
                            reversedEventId));
        }

        @Override
        public void saveRevision(
                OperationalEvent operationalEvent) {

            operations.add(
                    "save:"
                            + operationalEvent
                                    .id()
                                    .value());

            savedEvents.add(
                    operationalEvent);

            events.put(
                    operationalEvent.id(),
                    operationalEvent);
        }

        @Override
        public void appendStateTransition(
                EventStateTransition stateTransition) {

            operations.add(
                    "append-transition:"
                            + stateTransition
                                    .id()
                                    .value());

            transitions.add(
                    stateTransition);
        }

    }

}