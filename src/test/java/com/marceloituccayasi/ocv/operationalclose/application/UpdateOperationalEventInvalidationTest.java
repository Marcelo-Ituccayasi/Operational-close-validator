package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
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
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class UpdateOperationalEventInvalidationTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "ae956bc4-12c3-4554-8890-200ed3100001");

    private static final UUID ORIGINAL_EVENT_ID =
            UUID.fromString(
                    "ae956bc4-12c3-4554-8890-200ed3100002");

    private static final UUID CANCELLATION_EVENT_ID =
            UUID.fromString(
                    "ae956bc4-12c3-4554-8890-200ed3100003");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "ae956bc4-12c3-4554-8890-200ed3100004");

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-07-22T08:00:00Z");

    private static final Instant REVISED_AT =
            Instant.parse(
                    "2026-07-22T10:00:00Z");

    @Test
    void invalidatesOriginalAndDependentCancellationBeforeWrites() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore repository =
                new StubRevisionStore(
                        operations);

        OperationalEvent originalEvent =
                regularExpense();

        OperationalEvent cancellation =
                OperationalEvent.createCancellation(
                        new OperationalEventId(
                                CANCELLATION_EVENT_ID),
                        new OperationalCloseId(
                                CLOSE_ID),
                        originalEvent,
                        CREATED_AT.plusSeconds(120),
                        "Caja principal",
                        "Anulación inicial",
                        false,
                        false,
                        CREATED_AT.plusSeconds(180),
                        actor());

        repository.events.put(
                originalEvent.id(),
                originalEvent);

        repository.cancellations.put(
                originalEvent.id(),
                cancellation);

        List<OperationalEventId> invalidatedEventIds =
                new ArrayList<>();

        OperationalEventDependentResultInvalidator invalidator =
                (closeId, revisedEventIds) -> {
                    operations.add(
                            "invalidate:"
                                    + revisedEventIds.size());

                    assertThat(closeId)
                            .isEqualTo(
                                    new OperationalCloseId(
                                            CLOSE_ID));

                    invalidatedEventIds.addAll(
                            revisedEventIds);
                };

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        repository,
                        invalidator);

        UpdateOperationalEventResult result =
                useCase.execute(
                        updateCommand());

        assertThat(result.status())
                .isEqualTo(
                        UpdateOperationalEventResult.Status.UPDATED);

        assertThat(invalidatedEventIds)
                .containsExactly(
                        originalEvent.id(),
                        cancellation.id());

        assertThat(repository.savedEvents)
                .hasSize(2);

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + ORIGINAL_EVENT_ID,
                        "lock-cancellation:" + ORIGINAL_EVENT_ID,
                        "invalidate:2",
                        "save:" + ORIGINAL_EVENT_ID,
                        "save:" + CANCELLATION_EVENT_ID);
    }

    @Test
    void doesNotWriteRevisionsWhenInvalidationFails() {
        List<String> operations =
                new ArrayList<>();

        StubRevisionStore repository =
                new StubRevisionStore(
                        operations);

        OperationalEvent originalEvent =
                regularExpense();

        repository.events.put(
                originalEvent.id(),
                originalEvent);

        OperationalEventDependentResultInvalidator invalidator =
                (closeId, revisedEventIds) -> {
                    operations.add(
                            "invalidate:"
                                    + revisedEventIds.size());

                    throw new IllegalStateException(
                            "invalidation failure");
                };

        UpdateOperationalEvent useCase =
                newUseCase(
                        operations,
                        repository,
                        invalidator);

        assertThatThrownBy(
                () -> useCase.execute(
                        updateCommand()))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        "invalidation failure");

        assertThat(repository.savedEvents)
                .isEmpty();

        assertThat(repository.transitions)
                .isEmpty();

        assertThat(operations)
                .containsExactly(
                        "transaction",
                        "lock-close",
                        "lock-event:" + ORIGINAL_EVENT_ID,
                        "lock-cancellation:" + ORIGINAL_EVENT_ID,
                        "invalidate:1");
    }

    private static UpdateOperationalEvent newUseCase(
            List<String> operations,
            StubRevisionStore repository,
            OperationalEventDependentResultInvalidator invalidator) {

        OperationalClose operationalClose =
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

        OperationalCloseLockRepository closeRepository =
                closeId -> {
                    operations.add(
                            "lock-close");

                    if (!operationalClose.id()
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
                repository,
                currentActorProvider,
                applicationClock,
                uuidGenerator,
                transactionRunner,
                requirementPolicy,
                invalidator);
    }

    private static UpdateOperationalEventCommand updateCommand() {
        return new UpdateOperationalEventCommand(
                CLOSE_ID,
                ORIGINAL_EVENT_ID,
                "EXPENSE",
                new BigDecimal("125.2500"),
                null,
                REVISED_AT.minusSeconds(300),
                "Caja revisada",
                "Gasto operativo revisado");
    }

    private static OperationalEvent regularExpense() {
        return OperationalEvent.create(
                new OperationalEventId(
                        ORIGINAL_EVENT_ID),
                new OperationalCloseId(
                        CLOSE_ID),
                OperationalEventType.EXPENSE,
                new OperationalEventAmount(
                        new BigDecimal("80.0000")),
                CREATED_AT.plusSeconds(30),
                "Caja principal",
                "Gasto operativo inicial",
                false,
                false,
                CREATED_AT.plusSeconds(60),
                actor());
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