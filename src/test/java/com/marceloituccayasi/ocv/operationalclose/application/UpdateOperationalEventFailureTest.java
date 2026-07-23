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
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class UpdateOperationalEventFailureTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "e97ffea7-8e3e-4ccb-a96e-451081000001");

    private static final UUID OTHER_CLOSE_ID =
            UUID.fromString(
                    "e97ffea7-8e3e-4ccb-a96e-451081000002");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "e97ffea7-8e3e-4ccb-a96e-451081000003");

    private static final UUID TARGET_EVENT_ID =
            UUID.fromString(
                    "e97ffea7-8e3e-4ccb-a96e-451081000004");

    private static final UUID EXISTING_CANCELLATION_ID =
            UUID.fromString(
                    "e97ffea7-8e3e-4ccb-a96e-451081000005");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "e97ffea7-8e3e-4ccb-a96e-451081000006");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-22T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-22T10:00:00Z");

    @Test
    void rejectsUnauthorizedActorBeforeLockingClose() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        editableClose(),
                        "different-user");

        UpdateOperationalEventResult result =
                useCase.execute(
                        regularCommand());

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .ACTOR_REJECTED);

        assertThat(operations)
                .containsExactly(
                        "transaction");

        assertNoWrites(eventRepository);
    }

    @Test
    void returnsCloseNotFoundBeforeLockingEvent() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        null,
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        regularCommand());

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .CLOSE_NOT_FOUND);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close");

        assertNoWrites(eventRepository);
    }

    @Test
    void rejectsSentCloseBeforeLockingEvent() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        closeWithState(
                                OperationalCloseState
                                        .SENT_TO_ACCOUNTING),
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        regularCommand());

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .CLOSE_NOT_EDITABLE);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close");

        assertNoWrites(eventRepository);
    }

    @Test
    void returnsEventNotFoundWithoutDependentLocks() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        editableClose(),
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        regularCommand());

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .EVENT_NOT_FOUND);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID);

        assertNoWrites(eventRepository);
    }

    @Test
    void hidesEventThatBelongsToAnotherClose() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        OperationalEvent foreignEvent =
                regularEvent(
                        EVENT_ID,
                        OTHER_CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                foreignEvent.id(),
                foreignEvent);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        editableClose(),
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        regularCommand());

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .EVENT_NOT_FOUND);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID);

        assertNoWrites(eventRepository);
    }

    @Test
    void rejectsSelfCancellationBeforeLockingTargetAgain() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        OperationalEvent currentEvent =
                regularEvent(
                        EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                currentEvent.id(),
                currentEvent);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        editableClose(),
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                EVENT_ID,
                                "80.0000"));

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .INVALID_INPUT);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID,
                        "lock-cancellation:" + EVENT_ID);

        assertNoWrites(eventRepository);
    }

    @Test
    void rejectsCancellationWhenTargetAlreadyHasAnotherCancellation() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        OperationalEvent currentEvent =
                regularEvent(
                        EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.INCOME,
                        "80.0000");

        OperationalEvent targetEvent =
                regularEvent(
                        TARGET_EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        OperationalEvent existingCancellation =
                OperationalEvent.createCancellation(
                        new OperationalEventId(
                                EXISTING_CANCELLATION_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        targetEvent,
                        CREATED_AT.plusSeconds(30),
                        "Caja principal",
                        "Anulación existente",
                        false,
                        false,
                        CREATED_AT.plusSeconds(60),
                        actor());

        eventRepository.events.put(
                currentEvent.id(),
                currentEvent);

        eventRepository.events.put(
                targetEvent.id(),
                targetEvent);

        eventRepository.cancellations.put(
                targetEvent.id(),
                existingCancellation);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        editableClose(),
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                TARGET_EVENT_ID,
                                "80.0000"));

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .CANCELLATION_CONFLICT);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID,
                        "lock-cancellation:" + EVENT_ID,
                        "lock-event:" + TARGET_EVENT_ID,
                        "lock-cancellation:" + TARGET_EVENT_ID);

        assertNoWrites(eventRepository);
    }

    @Test
    void rejectsCancellationWithDifferentAmount() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore eventRepository =
                new StubRevisionStore(
                        operations);

        OperationalEvent currentEvent =
                regularEvent(
                        EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.INCOME,
                        "90.0000");

        OperationalEvent targetEvent =
                regularEvent(
                        TARGET_EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                currentEvent.id(),
                currentEvent);

        eventRepository.events.put(
                targetEvent.id(),
                targetEvent);

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        eventRepository,
                        editableClose(),
                        AuditActor.RESPONSIBLE_USER_ID);

        UpdateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                TARGET_EVENT_ID,
                                "90.0000"));

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status
                                .INVALID_INPUT);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + EVENT_ID,
                        "lock-cancellation:" + EVENT_ID,
                        "lock-event:" + TARGET_EVENT_ID,
                        "lock-cancellation:" + TARGET_EVENT_ID);

        assertNoWrites(eventRepository);
    }

    private static UpdateOperationalEvent newUseCase(
            List<String> operations,
            StubRevisionStore eventRepository,
            OperationalClose operationalClose,
            String actorUserId) {

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
                        actorUserId,
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

    private static UpdateOperationalEventCommand regularCommand() {
        return new UpdateOperationalEventCommand(
                CLOSE_ID,
                EVENT_ID,
                "EXPENSE",
                new BigDecimal("95.0000"),
                null,
                REVISED_AT.minusSeconds(300),
                "Caja revisada",
                "Evento operativo revisado");
    }

    private static UpdateOperationalEventCommand
            cancellationCommand(
                    UUID reversedEventId,
                    String amount) {

        return new UpdateOperationalEventCommand(
                CLOSE_ID,
                EVENT_ID,
                "CANCELLATION",
                new BigDecimal(amount),
                reversedEventId,
                REVISED_AT.minusSeconds(300),
                "Caja revisada",
                "Conversión a anulación");
    }

    private static OperationalClose editableClose() {
        return closeWithState(
                OperationalCloseState.PREPARATION);
    }

    private static OperationalClose closeWithState(
            OperationalCloseState state) {

        OperationalClose createdClose =
                OperationalClose.create(
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

        return new OperationalClose(
                createdClose.id(),
                createdClose.period(),
                createdClose.currencyCode(),
                createdClose.initialBalance(),
                state,
                CREATED_AT,
                createdClose.createdAt(),
                createdClose.createdBy(),
                CREATED_AT,
                createdClose.updatedBy());
    }

    private static OperationalEvent regularEvent(
            UUID eventId,
            UUID closeId,
            OperationalEventType eventType,
            String amount) {

        return OperationalEvent.create(
                new OperationalEventId(
                        eventId),
                new OperationalCloseId(
                        closeId),
                eventType,
                new OperationalEventAmount(
                        new BigDecimal(amount)),
                CREATED_AT.plusSeconds(10),
                "Caja principal",
                "Evento operativo inicial",
                false,
                false,
                CREATED_AT.plusSeconds(20),
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
                "responsible");
    }

    private static void assertNoWrites(
            StubRevisionStore repository) {

        assertThat(repository.savedEvents)
                .isEmpty();

        assertThat(repository.transitions)
                .isEmpty();
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