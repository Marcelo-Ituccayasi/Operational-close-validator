package com.marceloituccayasi.ocv.operationalclose.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationAlertRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlertChange;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertTransitionId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;

/**
 * Creates, maintains or resolves Validation Alerts from current Event
 * Validation Results.
 */
public final class EventValidationAlertSynchronizer {

    private final EventValidationAlertRepository
            alertRepository;

    private final UuidGenerator
            uuidGenerator;

    public EventValidationAlertSynchronizer(
            EventValidationAlertRepository alertRepository,
            UuidGenerator uuidGenerator) {

        this.alertRepository =
                Objects.requireNonNull(
                        alertRepository);

        this.uuidGenerator =
                Objects.requireNonNull(
                        uuidGenerator);
    }

    public void synchronize(
            List<EventValidationResult> validationResults,
            Instant occurredAt,
            AuditActor actor) {

        Objects.requireNonNull(
                validationResults,
                "validation results must not be null");

        Objects.requireNonNull(
                occurredAt,
                "alert synchronization instant must not be null");

        Objects.requireNonNull(
                actor,
                "alert synchronization actor must not be null");

        for (EventValidationResult validationResult
                : validationResults) {

            Objects.requireNonNull(
                    validationResult,
                    "validation result must not be null");

            if (!validationResult.current()) {
                throw new IllegalArgumentException(
                        "validation result must be current");
            }

            Optional<EventValidationAlert> openAlert =
                    alertRepository
                            .findOpenByEventIdAndCauseRuleCode(
                                    validationResult.eventId(),
                                    validationResult.ruleCode());

            if (validationResult.outcome()
                    == ValidationOutcome.FAILED) {

                if (openAlert.isEmpty()) {
                    createAlert(
                            validationResult,
                            occurredAt,
                            actor);
                }

                continue;
            }

            if (openAlert.isPresent()) {
                resolveAlert(
                        openAlert.orElseThrow(),
                        validationResult,
                        occurredAt,
                        actor);
            }
        }
    }

    public boolean hasOpenBlockingAlert(
            OperationalEventId eventId) {

        Objects.requireNonNull(
                eventId,
                "eventId must not be null");

        List<EventValidationAlert> openAlerts =
                Objects.requireNonNull(
                        alertRepository
                                .findAllOpenByEventIdOrderByCreatedAt(
                                        eventId),
                        "open validation alerts must not be null");

        for (EventValidationAlert openAlert
                : openAlerts) {

            Objects.requireNonNull(
                    openAlert,
                    "open validation alert must not be null");
        }

        return openAlerts.stream()
                .anyMatch(
                        EventValidationAlert::blocking);
    }

    private void createAlert(
            EventValidationResult failedResult,
            Instant occurredAt,
            AuditActor actor) {

        ValidationAlertId alertId =
                new ValidationAlertId(
                        nextUuid(
                                "generated validation alert UUID must not be null"));

        EventValidationAlert alert =
                EventValidationAlert
                        .createFromFailedResult(
                                alertId,
                                failedResult,
                                failedResult.detail(),
                                occurredAt,
                                actor);

        ValidationAlertTransition transition =
                ValidationAlertTransition.initial(
                        new ValidationAlertTransitionId(
                                nextUuid(
                                        "generated validation alert transition UUID must not be null")),
                        alertId,
                        failedResult.detail(),
                        occurredAt,
                        actor);

        alertRepository.saveNew(
                alert,
                transition);
    }

    private void resolveAlert(
            EventValidationAlert openAlert,
            EventValidationResult satisfiedResult,
            Instant occurredAt,
            AuditActor actor) {

        EventValidationAlert lockedAlert =
                alertRepository
                        .findByIdForUpdate(
                                openAlert.id())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "open validation alert disappeared while being locked"));

        EventValidationAlertChange change =
                lockedAlert.resolve(
                        new ValidationAlertTransitionId(
                                nextUuid(
                                        "generated validation alert transition UUID must not be null")),
                        satisfiedResult,
                        satisfiedResult.detail(),
                        occurredAt,
                        actor);

        alertRepository.saveChange(
                change);
    }

    private UUID nextUuid(
            String nullMessage) {

        return Objects.requireNonNull(
                uuidGenerator.next(),
                nullMessage);
    }

}