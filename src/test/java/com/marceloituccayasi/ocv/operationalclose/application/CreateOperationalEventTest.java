package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
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
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class CreateOperationalEventTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "6a56ca7b-d458-44b8-aa47-a3529d100001");

    private static final UUID OTHER_CLOSE_ID =
            UUID.fromString(
                    "6a56ca7b-d458-44b8-aa47-a3529d100002");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "6a56ca7b-d458-44b8-aa47-a3529d100003");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "6a56ca7b-d458-44b8-aa47-a3529d100004");

    private static final UUID ORIGINAL_EVENT_ID =
            UUID.fromString(
                    "6a56ca7b-d458-44b8-aa47-a3529d100005");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-22T06:00:00Z");

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-07-22T05:00:00Z");

    private static final AuditActor ACTOR =
            new AuditActor(
                    "responsible-user",
                    "responsible");

    @Test
    void createsIncomeAndInitialTransitionAtomically() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        DirectTransactionRunner transactionRunner =
                new DirectTransactionRunner(operations);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        transactionRunner,
                        "responsible-user",
                        new OperationalEventRequirements(
                                true,
                                false));

        CreateOperationalEventResult result =
                useCase.execute(incomeCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.CREATED);

        assertThat(result.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(eventRepository.savedEvent)
                .isNotNull();

        assertThat(eventRepository.savedEvent.id().value())
                .isEqualTo(EVENT_ID);

        assertThat(eventRepository.savedEvent.closeId().value())
                .isEqualTo(CLOSE_ID);

        assertThat(eventRepository.savedEvent.eventType())
                .isEqualTo(OperationalEventType.INCOME);

        assertThat(eventRepository.savedEvent.amount().value())
                .isEqualByComparingTo("125.5000");

        assertThat(eventRepository.savedEvent.balanceEffect())
                .isEqualByComparingTo("125.5000");

        assertThat(eventRepository.savedEvent.state())
                .isEqualTo(OperationalEventState.REGISTERED);

        assertThat(eventRepository.savedEvent.evidenceRequired())
                .isTrue();

        assertThat(eventRepository.savedEvent.authorizationRequired())
                .isFalse();

        assertThat(eventRepository.savedTransition)
                .isNotNull();

        assertThat(eventRepository.savedTransition.id().value())
                .isEqualTo(TRANSITION_ID);

        assertThat(eventRepository.savedTransition.fromState())
                .isNull();

        assertThat(eventRepository.savedTransition.toState())
                .isEqualTo(OperationalEventState.REGISTERED);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "save-event");
    }

    @Test
    void createsCancellationForEventFromSameClose() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.BLOCKED),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        OperationalEvent original =
                regularEvent(
                        ORIGINAL_EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                original.id(),
                original);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                true));

        CreateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                new BigDecimal("80.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.CREATED);

        assertThat(eventRepository.savedEvent.eventType())
                .isEqualTo(
                        OperationalEventType.CANCELLATION);

        assertThat(eventRepository.savedEvent.reversedEventId())
                .isEqualTo(original.id());

        assertThat(eventRepository.savedEvent.amount().value())
                .isEqualByComparingTo("80.0000");

        assertThat(eventRepository.savedEvent.balanceEffect())
                .isEqualByComparingTo("80.0000");

        assertThat(eventRepository.savedEvent.authorizationRequired())
                .isTrue();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "find-event",
                        "check-cancellation",
                        "save-event");
    }

    @Test
    void rejectsActorBeforeLockingClose() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "another-user",
                        new OperationalEventRequirements(
                                false,
                                false));

        CreateOperationalEventResult result =
                useCase.execute(incomeCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.ACTOR_REJECTED);

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly("transaction");
    }

    @Test
    void reportsCloseNotFoundWithoutPersisting() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        null,
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                false));

        CreateOperationalEventResult result =
                useCase.execute(incomeCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.CLOSE_NOT_FOUND);

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close");
    }

    @Test
    void rejectsCreationWhenCloseWasSentToAccounting() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.SENT_TO_ACCOUNTING),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                false));

        CreateOperationalEventResult result =
                useCase.execute(incomeCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.CLOSE_NOT_EDITABLE);

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close");
    }

    @Test
    void reportsMissingReversedEvent() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                true));

        CreateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                new BigDecimal("80.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status
                                .REVERSED_EVENT_NOT_FOUND);

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "find-event");
    }

    @Test
    void rejectsSecondCancellationForSameEvent() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        OperationalEvent original =
                regularEvent(
                        ORIGINAL_EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.INCOME,
                        "80.0000");

        eventRepository.events.put(
                original.id(),
                original);

        eventRepository.cancellationExists =
                true;

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                true));

        CreateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                new BigDecimal("80.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status
                                .CANCELLATION_CONFLICT);

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "find-event",
                        "check-cancellation");
    }

    @Test
    void rejectsCancellationWithDifferentAmount() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        OperationalEvent original =
                regularEvent(
                        ORIGINAL_EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                original.id(),
                original);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                true));

        CreateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                new BigDecimal("75.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.INVALID_INPUT);

        assertThat(result.message())
                .contains(
                        "must equal reversed event amount");

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "find-event");
    }

    @Test
    void rejectsCancellationAcrossDifferentCloses() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        OperationalEvent original =
                regularEvent(
                        ORIGINAL_EVENT_ID,
                        OTHER_CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                original.id(),
                original);

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                true));

        CreateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                new BigDecimal("80.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status.INVALID_INPUT);

        assertThat(result.message())
                .contains("same close");

        assertThat(eventRepository.savedEvent)
                .isNull();
    }

    @Test
    void translatesConcurrentCancellationConflict() {
        List<String> operations =
                new ArrayList<>();

        StubCloseLockPort closeRepository =
                new StubCloseLockPort(
                        close(
                                CLOSE_ID,
                                OperationalCloseState.PREPARATION),
                        operations);

        StubEventStore eventRepository =
                new StubEventStore(operations);

        OperationalEvent original =
                regularEvent(
                        ORIGINAL_EVENT_ID,
                        CLOSE_ID,
                        OperationalEventType.EXPENSE,
                        "80.0000");

        eventRepository.events.put(
                original.id(),
                original);

        eventRepository.failWithDuplicateCancellationOnSave =
                true;

        CreateOperationalEvent useCase =
                newUseCase(
                        closeRepository,
                        eventRepository,
                        new DirectTransactionRunner(operations),
                        "responsible-user",
                        new OperationalEventRequirements(
                                false,
                                true));

        CreateOperationalEventResult result =
                useCase.execute(
                        cancellationCommand(
                                new BigDecimal("80.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalEventResult.Status
                                .CANCELLATION_CONFLICT);

        assertThat(eventRepository.savedEvent)
                .isNull();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "find-event",
                        "check-cancellation",
                        "save-event");
    }

    private static CreateOperationalEvent newUseCase(
            StubCloseLockPort closeRepository,
            StubEventStore eventRepository,
            DirectTransactionRunner transactionRunner,
            String actoruserId,
            OperationalEventRequirements requirements) {

        return new CreateOperationalEvent(
                closeRepository,
                eventRepository,
                () -> new AuthenticatedPrincipal(
                        actoruserId,
                        "responsible"),
                () -> NOW,
                new SequenceUuidGenerator(
                        EVENT_ID,
                        TRANSITION_ID),
                transactionRunner,
                (eventType, amount) -> requirements);
    }

    private static CreateOperationalEventCommand incomeCommand() {
        return new CreateOperationalEventCommand(
                CLOSE_ID,
                "INCOME",
                new BigDecimal("125.5000"),
                null,
                OCCURRED_AT,
                "Caja principal",
                "Ingreso de venta");
    }

    private static CreateOperationalEventCommand
            cancellationCommand(
                    BigDecimal amount) {

        return new CreateOperationalEventCommand(
                CLOSE_ID,
                "CANCELLATION",
                amount,
                ORIGINAL_EVENT_ID,
                OCCURRED_AT,
                "Caja principal",
                "Anulación de evento");
    }

    private static OperationalClose close(
            UUID closeId,
            OperationalCloseState state) {

        return new OperationalClose(
                new OperationalCloseId(closeId),
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("1000.0000")),
                state,
                NOW,
                NOW,
                ACTOR,
                NOW,
                ACTOR);
    }

    private static OperationalEvent regularEvent(
            UUID eventId,
            UUID closeId,
            OperationalEventType eventType,
            String amount) {

        return OperationalEvent.create(
                new OperationalEventId(eventId),
                new OperationalCloseId(closeId),
                eventType,
                new OperationalEventAmount(
                        new BigDecimal(amount)),
                OCCURRED_AT,
                "Caja principal",
                "Evento original",
                false,
                false,
                NOW,
                ACTOR);
    }

    private static final class StubCloseLockPort
            implements OperationalCloseLockRepository {

        private final OperationalClose operationalClose;
        private final List<String> operations;

        private StubCloseLockPort(
                OperationalClose operationalClose,
                List<String> operations) {

            this.operationalClose =
                    operationalClose;

            this.operations =
                    operations;
        }

        @Override
        public Optional<OperationalClose> findByIdForUpdate(
                OperationalCloseId closeId) {

            operations.add("lock-close");

            if (operationalClose == null) {
                return Optional.empty();
            }

            return Optional.of(operationalClose);
        }

    }

   private static final class StubEventStore
        implements OperationalEventRepository {

    private final Map<OperationalEventId, OperationalEvent>
            events =
                    new HashMap<>();

    private final List<String> operations;

    private boolean cancellationExists;

    private boolean failWithDuplicateCancellationOnSave;

    private OperationalEvent savedEvent;

    private EventStateTransition savedTransition;

    private StubEventStore(
            List<String> operations) {

        this.operations =
                operations;
    }

    @Override
    public void saveNew(
            OperationalEvent operationalEvent,
            EventStateTransition initialTransition) {

        operations.add("save-event");

        if (failWithDuplicateCancellationOnSave) {
            throw new DuplicateOperationalEventCancellationException();
        }

        savedEvent =
                operationalEvent;

        savedTransition =
                initialTransition;

        events.put(
                operationalEvent.id(),
                operationalEvent);
    }

    @Override
    public Optional<OperationalEvent> findById(
            OperationalEventId eventId) {

        operations.add("find-event");

        return Optional.ofNullable(
                events.get(eventId));
    }

    @Override
    public List<OperationalEvent>
            findAllByCloseIdOrderByOccurredAtDescending(
                    OperationalCloseId closeId) {

        return events.values()
                .stream()
                .filter(operationalEvent ->
                        operationalEvent
                                .closeId()
                                .equals(closeId))
                .sorted((left, right) -> {
                    int occurredComparison =
                            right.occurredAt()
                                    .compareTo(
                                            left.occurredAt());

                    if (occurredComparison != 0) {
                        return occurredComparison;
                    }

                    int registeredComparison =
                            right.registeredAt()
                                    .compareTo(
                                            left.registeredAt());

                    if (registeredComparison != 0) {
                        return registeredComparison;
                    }

                    return right.id()
                            .value()
                            .compareTo(
                                    left.id().value());
                })
                .toList();
    }

    @Override
    public boolean existsCancellationFor(
            OperationalEventId reversedEventId) {

        operations.add("check-cancellation");

        return cancellationExists;
    }

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

            operations.add("transaction");

            return operation.get();
        }

    }

    private static final class SequenceUuidGenerator
            implements UuidGenerator {

        private final Deque<UUID> values;

        private SequenceUuidGenerator(
                UUID... values) {

            values =
                    values.clone();

            this.values =
                    new ArrayDeque<>(
                            List.of(values));
        }

        @Override
        public UUID next() {
            return values.removeFirst();
        }

    }
}