package com.marceloituccayasi.ocv.operationalclose.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

class CreateOperationalCloseTest {

    private static final UUID CLOSE_ID =
            UUID.fromString(
                    "6c13876e-b9fe-49f8-a73b-1d32cbb01738");

    private static final UUID TRANSITION_ID =
            UUID.fromString(
                    "96bec50f-6525-46cb-b3b1-c5a7185a22db");

    private static final Instant NOW =
            Instant.parse(
                    "2026-07-20T05:00:00Z");

    @Test
    void createsCloseAndInitialTransitionAtomically() {
        StubOperationalClosePort repository =
                new StubOperationalClosePort();

        DirectTransactionRunner transactionRunner =
                new DirectTransactionRunner();

        CreateOperationalClose useCase =
                new CreateOperationalClose(
                        repository,
                        () -> new AuthenticatedPrincipal(
                                "responsible-user",
                                "responsible"),
                        () -> NOW,
                        new SequenceUuidGenerator(
                                CLOSE_ID,
                                TRANSITION_ID),
                        transactionRunner);

        CreateOperationalCloseResult result =
                useCase.execute(validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalCloseResult.Status.CREATED);

        assertThat(result.closeId())
                .isEqualTo(CLOSE_ID);

        assertThat(repository.savedClose)
                .isNotNull();

        assertThat(repository.savedClose.id().value())
                .isEqualTo(CLOSE_ID);

        assertThat(repository.savedClose.state())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(repository.savedClose.createdAt())
                .isEqualTo(NOW);

        assertThat(repository.savedTransition)
                .isNotNull();

        assertThat(repository.savedTransition.id().value())
                .isEqualTo(TRANSITION_ID);

        assertThat(repository.savedTransition.fromState())
                .isNull();

        assertThat(repository.savedTransition.toState())
                .isEqualTo(
                        OperationalCloseState.PREPARATION);

        assertThat(transactionRunner.invocations)
                .isEqualTo(1);
    }

    @Test
    void rejectsInvalidPeriodWithoutPersisting() {
        StubOperationalClosePort repository =
                new StubOperationalClosePort();

        CreateOperationalClose useCase =
                newUseCase(
                        repository,
                        "responsible-user");

        CreateOperationalCloseResult result =
                useCase.execute(
                        new CreateOperationalCloseCommand(
                                LocalDate.of(2026, 7, 31),
                                LocalDate.of(2026, 7, 1),
                                "PEN",
                                new BigDecimal("100.0000")));

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalCloseResult.Status.INVALID_INPUT);

        assertThat(repository.savedClose)
                .isNull();
    }

    @Test
    void rejectsActorOtherThanResponsibleUser() {
        StubOperationalClosePort repository =
                new StubOperationalClosePort();

        CreateOperationalClose useCase =
                newUseCase(
                        repository,
                        "another-user");

        CreateOperationalCloseResult result =
                useCase.execute(validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalCloseResult.Status.ACTOR_REJECTED);

        assertThat(repository.savedClose)
                .isNull();
    }

    @Test
    void reportsPeriodDetectedBeforePersistence() {
        StubOperationalClosePort repository =
                new StubOperationalClosePort();

        repository.periodAlreadyExists =
                true;

        CreateOperationalClose useCase =
                newUseCase(
                        repository,
                        "responsible-user");

        CreateOperationalCloseResult result =
                useCase.execute(validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalCloseResult.Status.PERIOD_CONFLICT);

        assertThat(repository.savedClose)
                .isNull();
    }

    @Test
    void translatesConcurrentUniqueConflict() {
        StubOperationalClosePort repository =
                new StubOperationalClosePort();

        repository.failWithDuplicateOnSave =
                true;

        CreateOperationalClose useCase =
                newUseCase(
                        repository,
                        "responsible-user");

        CreateOperationalCloseResult result =
                useCase.execute(validCommand());

        assertThat(result.status())
                .isEqualTo(
                        CreateOperationalCloseResult.Status.PERIOD_CONFLICT);

        assertThat(repository.saveAttempts)
                .isEqualTo(1);
    }

    private static CreateOperationalClose newUseCase(
            StubOperationalClosePort repository,
            String userId) {

        return new CreateOperationalClose(
                repository,
                () -> new AuthenticatedPrincipal(
                        userId,
                        "responsible"),
                () -> NOW,
                new SequenceUuidGenerator(
                        CLOSE_ID,
                        TRANSITION_ID),
                new DirectTransactionRunner());
    }

    private static CreateOperationalCloseCommand validCommand() {
        return new CreateOperationalCloseCommand(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "PEN",
                new BigDecimal("100.0000"));
    }

    private static final class SequenceUuidGenerator
            implements UuidGenerator {

        private final Deque<UUID> values;

        private SequenceUuidGenerator(
                UUID... values) {

            this.values =
                    new ArrayDeque<>(
                            List.of(values));
        }

        @Override
        public UUID next() {
            return values.removeFirst();
        }

    }

    private static final class DirectTransactionRunner
            implements TransactionRunner {

        private int invocations;

        @Override
        public <T> T execute(
                Supplier<T> operation) {

            invocations++;
            return operation.get();
        }

    }

    private static final class StubOperationalClosePort
            implements OperationalCloseRepository {

        private boolean periodAlreadyExists;
        private boolean failWithDuplicateOnSave;
        private int saveAttempts;
        private OperationalClose savedClose;
        private CloseStateTransition savedTransition;
        private final List<OperationalClose> closes =
                new ArrayList<>();

        @Override
        public boolean existsByPeriod(
                OperationalPeriod period) {

            return periodAlreadyExists;
        }

        @Override
        public void saveNew(
                OperationalClose operationalClose,
                CloseStateTransition initialTransition) {

            saveAttempts++;

            if (failWithDuplicateOnSave) {
                throw new DuplicateOperationalClosePeriodException();
            }

            savedClose =
                    operationalClose;

            savedTransition =
                    initialTransition;

            closes.add(operationalClose);
        }

        @Override
        public Optional<OperationalClose> findById(
                OperationalCloseId closeId) {

            return closes.stream()
                    .filter(
                            operationalClose ->
                                    operationalClose.id()
                                            .equals(closeId))
                    .findFirst();
        }

        @Override
        public List<OperationalClose> findAllByPeriodDescending() {
            return List.copyOf(closes);
        }

    }

}
