package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class ListOperationalEventsAccessTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "e79d78b1-c048-44bf-881b-8d780c660001");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-23T12:00:00Z");

    @Test
    void exposesPreparationCloseAsEditable() {
        EventQueryPort eventPort =
                new EventQueryPort();

        ListOperationalEvents useCase =
                new ListOperationalEvents(
                        new CloseQueryPort(
                                closeWithState(
                                        OperationalCloseState.PREPARATION)),
                        eventPort,
                        new DirectTransactionRunner());

        ListOperationalEventsResult result =
                useCase.execute(
                        CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        ListOperationalEventsResult.Status.FOUND);

        assertThat(result.closeState())
                .isEqualTo(
                        "PREPARATION");

        assertThat(result.closeEditable())
                .isTrue();

        assertThat(result.operationalEvents())
                .isEmpty();

        assertThat(eventPort.listInvocations)
                .isEqualTo(1);
    }

    @Test
    void exposesValidatedCloseAsEditable() {
        ListOperationalEventsResult result =
                new ListOperationalEvents(
                        new CloseQueryPort(
                                closeWithState(
                                        OperationalCloseState.VALIDATED)),
                        new EventQueryPort(),
                        new DirectTransactionRunner())
                        .execute(
                                CLOSE_ID);

        assertThat(result.closeState())
                .isEqualTo(
                        "VALIDATED");

        assertThat(result.closeEditable())
                .isTrue();
    }

    @Test
    void exposesSentCloseAsNotEditable() {
        ListOperationalEventsResult result =
                new ListOperationalEvents(
                        new CloseQueryPort(
                                closeWithState(
                                        OperationalCloseState
                                                .SENT_TO_ACCOUNTING)),
                        new EventQueryPort(),
                        new DirectTransactionRunner())
                        .execute(
                                CLOSE_ID);

        assertThat(result.closeState())
                .isEqualTo(
                        "SENT_TO_ACCOUNTING");

        assertThat(result.closeEditable())
                .isFalse();
    }

    @Test
    void closeNotFoundContainsNoAccessProjection() {
        EventQueryPort eventPort =
                new EventQueryPort();

        ListOperationalEventsResult result =
                new ListOperationalEvents(
                        new CloseQueryPort(
                                null),
                        eventPort,
                        new DirectTransactionRunner())
                        .execute(
                                CLOSE_ID);

        assertThat(result.status())
                .isEqualTo(
                        ListOperationalEventsResult.Status
                                .CLOSE_NOT_FOUND);

        assertThat(result.closeState())
                .isNull();

        assertThat(result.closeEditable())
                .isFalse();

        assertThat(result.operationalEvents())
                .isEmpty();

        assertThat(eventPort.listInvocations)
                .isZero();
    }

    private static OperationalClose closeWithState(
            OperationalCloseState state) {

        return new OperationalClose(
                new OperationalCloseId(
                        CLOSE_ID),
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
                NOW,
                NOW,
                actor(),
                NOW,
                actor());
    }

    private static AuditActor actor() {
        return new AuditActor(
                AuditActor.RESPONSIBLE_USER_ID,
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

    private static final class CloseQueryPort
            implements OperationalCloseRepository {

        private final OperationalClose operationalClose;

        private CloseQueryPort(
                OperationalClose operationalClose) {

            this.operationalClose =
                    operationalClose;
        }

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

            if (operationalClose == null
                    || !operationalClose
                            .id()
                            .equals(closeId)) {

                return Optional.empty();
            }

            return Optional.of(
                    operationalClose);
        }

        @Override
        public List<OperationalClose>
                findAllByPeriodDescending() {

            return operationalClose == null
                    ? List.of()
                    : List.of(
                            operationalClose);
        }

    }

    private static final class EventQueryPort
            implements OperationalEventRepository {

        private int listInvocations;

        @Override
        public void saveNew(
                OperationalEvent operationalEvent,
                EventStateTransition initialTransition) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OperationalEvent> findById(
                OperationalEventId eventId) {

            return Optional.empty();
        }

        @Override
        public List<OperationalEvent>
                findAllByCloseIdOrderByOccurredAtDescending(
                        OperationalCloseId closeId) {

            listInvocations++;
            return List.of();
        }

        @Override
        public boolean existsCancellationFor(
                OperationalEventId reversedEventId) {

            return false;
        }

    }

}