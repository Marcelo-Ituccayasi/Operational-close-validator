package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;

class GetOperationalEventDetailTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "9364de64-fc34-43c6-bf0d-b8d3a9200001");

    private static final UUID OTHER_CLOSE_ID =
            UUID.fromString(
                    "9364de64-fc34-43c6-bf0d-b8d3a9200002");

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "9364de64-fc34-43c6-bf0d-b8d3a9200003");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-22T09:00:00Z");

    @Test
    void returnsMappedDetailWhenEventBelongsToClose() {
        OperationalEvent operationalEvent =
                operationalEvent(
                        CLOSE_ID);

        GetOperationalEventDetail useCase =
                new GetOperationalEventDetail(
                        new QueryOperationalEventPort(
                                List.of(
                                        operationalEvent)),
                        new DirectTransactionRunner());

        GetOperationalEventResult result =
                useCase.execute(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalEventResult.Status.FOUND);

        assertThat(result.operationalEvent().id())
                .isEqualTo(EVENT_ID);

        assertThat(result.operationalEvent().closeId())
                .isEqualTo(CLOSE_ID);

        assertThat(result.operationalEvent().eventType())
                .isEqualTo("DISCOUNT");

        assertThat(result.operationalEvent().amount())
                .isEqualByComparingTo("25.0000");

        assertThat(result.operationalEvent().balanceEffect())
                .isEqualByComparingTo("-25.0000");

        assertThat(result.operationalEvent().dataRevision())
                .isEqualTo(1L);

        assertThat(result.operationalEvent().createdByUserId())
                .isEqualTo("responsible-user");
    }

    @Test
    void returnsNotFoundWhenEventDoesNotExist() {
        GetOperationalEventDetail useCase =
                new GetOperationalEventDetail(
                        new QueryOperationalEventPort(
                                List.of()),
                        new DirectTransactionRunner());

        GetOperationalEventResult result =
                useCase.execute(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalEventResult.Status.NOT_FOUND);

        assertThat(result.operationalEvent())
                .isNull();
    }

    @Test
    void returnsNotFoundWhenEventBelongsToAnotherClose() {
        OperationalEvent operationalEvent =
                operationalEvent(
                        OTHER_CLOSE_ID);

        GetOperationalEventDetail useCase =
                new GetOperationalEventDetail(
                        new QueryOperationalEventPort(
                                List.of(
                                        operationalEvent)),
                        new DirectTransactionRunner());

        GetOperationalEventResult result =
                useCase.execute(
                        CLOSE_ID,
                        EVENT_ID);

        assertThat(result.status())
                .isEqualTo(
                        GetOperationalEventResult.Status.NOT_FOUND);

        assertThat(result.operationalEvent())
                .isNull();
    }

    private static OperationalEvent operationalEvent(
            UUID closeId) {

        return OperationalEvent.create(
                new OperationalEventId(EVENT_ID),
                new OperationalCloseId(closeId),
                OperationalEventType.DISCOUNT,
                new OperationalEventAmount(
                        new BigDecimal("25.0000")),
                NOW.minusSeconds(60),
                "Caja principal",
                "Descuento consultado",
                false,
                false,
                NOW,
                new AuditActor(
                        "responsible-user",
                        "responsible"));
    }

    private static final class DirectTransactionRunner
            implements TransactionRunner {

        @Override
        public <T> T execute(
                Supplier<T> operation) {

            return operation.get();
        }

    }

    private record QueryOperationalEventPort(
            List<OperationalEvent> events)
            implements OperationalEventRepository {

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