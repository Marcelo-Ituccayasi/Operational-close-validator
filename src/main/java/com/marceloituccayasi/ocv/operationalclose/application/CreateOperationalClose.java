package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

/**
 * Creates an Operational Close and its initial state transition atomically.
 */
public final class CreateOperationalClose {

    private final OperationalCloseRepository repository;
    private final CurrentActorProvider currentActorProvider;
    private final ApplicationClock applicationClock;
    private final UuidGenerator uuidGenerator;
    private final TransactionRunner transactionRunner;

    public CreateOperationalClose(
            OperationalCloseRepository repository,
            CurrentActorProvider currentActorProvider,
            ApplicationClock applicationClock,
            UuidGenerator uuidGenerator,
            TransactionRunner transactionRunner) {

        this.repository =
                Objects.requireNonNull(repository);

        this.currentActorProvider =
                Objects.requireNonNull(currentActorProvider);

        this.applicationClock =
                Objects.requireNonNull(applicationClock);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);

        this.transactionRunner =
                Objects.requireNonNull(transactionRunner);
    }

    public CreateOperationalCloseResult execute(
            CreateOperationalCloseCommand command) {

        Objects.requireNonNull(
                command,
                "command must not be null");

        try {
            return transactionRunner.execute(
                    () -> executeInsideTransaction(command));
        }
        catch (DuplicateOperationalClosePeriodException exception) {
            return CreateOperationalCloseResult.periodConflict();
        }
    }

    private CreateOperationalCloseResult executeInsideTransaction(
            CreateOperationalCloseCommand command) {

        AuthenticatedPrincipal principal =
                currentActorProvider.currentActor();

        if (!AuditActor.RESPONSIBLE_USER_ID.equals(
                principal.userId())) {

            return CreateOperationalCloseResult.actorRejected();
        }

        AuditActor actor;
        OperationalPeriod period;
        CurrencyCode currencyCode;
        InitialBalance initialBalance;

        try {
            actor =
                    new AuditActor(
                            principal.userId(),
                            principal.username());

            period =
                    new OperationalPeriod(
                            command.periodStart(),
                            command.periodEnd());

            currencyCode =
                    new CurrencyCode(
                            command.currencyCode());

            initialBalance =
                    new InitialBalance(
                            command.initialBalance());
        }
        catch (IllegalArgumentException exception) {
            return CreateOperationalCloseResult.invalidInput(
                    exception.getMessage());
        }

        if (repository.existsByPeriod(period)) {
            return CreateOperationalCloseResult.periodConflict();
        }

        UUID closeUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated close UUID must not be null");

        UUID transitionUuid =
                Objects.requireNonNull(
                        uuidGenerator.next(),
                        "generated transition UUID must not be null");

        Instant now =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application time must not be null");

        OperationalCloseId closeId =
                new OperationalCloseId(closeUuid);

        OperationalClose operationalClose =
                OperationalClose.create(
                        closeId,
                        period,
                        currencyCode,
                        initialBalance,
                        now,
                        actor);

        CloseStateTransition initialTransition =
                CloseStateTransition.initial(
                        new CloseStateTransitionId(
                                transitionUuid),
                        closeId,
                        now,
                        actor);

        repository.saveNew(
                operationalClose,
                initialTransition);

        return CreateOperationalCloseResult.created(
                closeUuid);
    }

}
