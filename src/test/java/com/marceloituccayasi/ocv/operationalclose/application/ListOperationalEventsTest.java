package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
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

class ListOperationalEventsTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "8a5bf41f-90c5-45d8-822c-82902ca00001");

    private static final UUID FIRST_EVENT_ID =
            UUID.fromString(
                    "8a5bf41f-90c5-45d8-822c-82902ca00002");

    private static final UUID SECOND_EVENT_ID =
            UUID.fromString(
                    "8a5bf41f-90c5-45d8-822c-82902ca00003");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-22T09:00:00Z");

    @Test
    void returnsMappedEventsInRepositoryOrder() {
        OperationalClose operationalClose =
                operationalClose();

        OperationalEvent firstEvent =
                operationalEvent(
                        FIRST_EVENT_ID,
                        OperationalEventType.INCOME,
                        "150.0000",
                        NOW.minusSeconds(60));

        OperationalEvent secondEvent =
                operationalEvent(
                        SECOND_EVENT_ID,
                        OperationalEventType.EXPENSE,
                        "40.0000",
                        NOW.minusSeconds(120));

        QueryOperationalEventPort eventRepository =
                new QueryOperationalEventPort(
                        List.of(
                                firstEvent,
                                secondEvent));

        ListOperationalEvents useCase =
                new ListOperationalEvents(
                        new QueryOperationalClosePort(
                                List.of(operationalClose)),
                        eventRepository,
                        new DirectTransactionRunner());

        ListOperationalEventsResult result =
                useCase.execute(CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        ListOperationalEventsResult.Status.FOUND);

        assertThat(result.operationalEvents())
                .extracting(
                        OperationalEventView::id)
                .containsExactly(
                        FIRST_EVENT_ID,
                        SECOND_EVENT_ID);

        assertThat(result.operationalEvents())
                .extracting(
                        OperationalEventView::eventType)
                .containsExactly(
                        "INCOME",
                        "EXPENSE");

        assertThat(result.operationalEvents().getFirst().balanceEffect())
                .isEqualByComparingTo("150.0000");

        assertThat(result.operationalEvents().get(1).balanceEffect())
                .isEqualByComparingTo("-40.0000");

        assertThat(eventRepository.listInvocations)
                .isEqualTo(1);
    }

    @Test
    void returnsFoundWithEmptyListWhenCloseExistsWithoutEvents() {
        ListOperationalEvents useCase =
                new ListOperationalEvents(
                        new QueryOperationalClosePort(
                                List.of(
                                        operationalClose())),
                        new QueryOperationalEventPort(
                                List.of()),
                        new DirectTransactionRunner());

        ListOperationalEventsResult result =
                useCase.execute(CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        ListOperationalEventsResult.Status.FOUND);

        assertThat(result.operationalEvents())
                .isEmpty();
    }

    @Test
    void returnsCloseNotFoundWithoutQueryingEvents() {
        QueryOperationalEventPort eventRepository =
                new QueryOperationalEventPort(
                        List.of());

        ListOperationalEvents useCase =
                new ListOperationalEvents(
                        new QueryOperationalClosePort(
                                List.of()),
                        eventRepository,
                        new DirectTransactionRunner());

        ListOperationalEventsResult result =
                useCase.execute(CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        ListOperationalEventsResult.Status
                                .CLOSE_NOT_FOUND);

        assertThat(result.operationalEvents())
                .isEmpty();

        assertThat(eventRepository.listInvocations)
                .isZero();
    }

    @Test
    void returnsAnUnmodifiableList() {
        ListOperationalEvents useCase =
                new ListOperationalEvents(
                        new QueryOperationalClosePort(
                                List.of(
                                        operationalClose())),
                        new QueryOperationalEventPort(
                                List.of()),
                        new DirectTransactionRunner());

        ListOperationalEventsResult result =
                useCase.execute(CLOSE_ID);

        assertThatThrownBy(
                result.operationalEvents()::clear)
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    private static OperationalClose operationalClose() {
        return OperationalClose.create(
                new OperationalCloseId(CLOSE_ID),
                new OperationalPeriod(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)),
                new CurrencyCode("PEN"),
                new InitialBalance(
                        new BigDecimal("1000.0000")),
                NOW,
                actor());
    }

    private static OperationalEvent operationalEvent(
            UUID eventId,
            OperationalEventType eventType,
            String amount,
            Instant occurredAt) {

        return OperationalEvent.create(
                new OperationalEventId(eventId),
                new OperationalCloseId(CLOSE_ID),
                eventType,
                new OperationalEventAmount(
                        new BigDecimal(amount)),
                occurredAt,
                "Caja principal",
                "Evento de consulta",
                false,
                false,
                NOW,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                "responsible-user",
                "responsible");
    }

    private static final class DirectTransactionRunner
            implements TransactionRunner {

        @Override
        public <T> T execute(
                Supplier<T> operation) {

            return operation.get();
        }

    }

    private record QueryOperationalClosePort(
            List<OperationalClose> closes)
            implements OperationalCloseRepository {

        @Override
        public boolean existsByPeriod(
                OperationalPeriod period) {

            return false;
        }

        @Override
        public void saveNew(
                OperationalClose operationalClose,
                CloseStateTransition initialTransition) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OperationalClose> findById(
                OperationalCloseId closeId) {

            return closes.stream()
                    .filter(operationalClose ->
                            operationalClose.id()
                                    .equals(closeId))
                    .findFirst();
        }

        @Override
        public List<OperationalClose>
                findAllByPeriodDescending() {

            return List.copyOf(closes);
        }

    }

    private static final class QueryOperationalEventPort
            implements OperationalEventRepository {

        private final List<OperationalEvent> events;

        private int listInvocations;

        private QueryOperationalEventPort(
                List<OperationalEvent> events) {

            this.events =
                    List.copyOf(events);
        }

        @Override
        public void saveNew(
                OperationalEvent operationalEvent,
                EventStateTransition initialTransition) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OperationalEvent> findById(
                OperationalEventId eventId) {

            return events.stream()
                    .filter(operationalEvent ->
                            operationalEvent.id()
                                    .equals(eventId))
                    .findFirst();
        }

        @Override
        public List<OperationalEvent>
                findAllByCloseIdOrderByOccurredAtDescending(
                        OperationalCloseId closeId) {

            listInvocations++;

            return events.stream()
                    .filter(operationalEvent ->
                            operationalEvent.closeId()
                                    .equals(closeId))
                    .toList();
        }

        @Override
        public boolean existsCancellationFor(
                OperationalEventId reversedEventId) {

            return false;
        }

    }

}