package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationEventSnapshot;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationAlert;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventState;
import com.marceloituccayasi.ocv.operationalclose.domain.SubmissionAttemptIssueType;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationAlertId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationOutcome;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;

/**
 * Pure final-control evaluator for VR-008.
 *
 * <p>All arguments must be reloaded after the owning Operational Close has
 * been locked. This component performs no persistence and no state changes.</p>
 */
public final class Vr008Evaluator {

    public Vr008Evaluation evaluate(
            OperationalCloseId closeId,
            List<OperationalEvent> events,
            CloseConsolidationReadiness readiness,
            List<EventValidationResult> currentValidationResults,
            List<EventValidationAlert> openAlerts,
            Consolidation consolidation) {

        requireNonNull(
                closeId,
                "closeId");

        requireNonNull(
                events,
                "events");

        requireNonNull(
                readiness,
                "readiness");

        requireNonNull(
                currentValidationResults,
                "currentValidationResults");

        requireNonNull(
                openAlerts,
                "openAlerts");

        Map<OperationalEventId, OperationalEvent> eventsById =
                validateAndIndexEvents(
                        closeId,
                        events);

        validateReadiness(
                readiness,
                eventsById);

        Map<OperationalEventId, List<EventValidationResult>>
                resultsByEvent =
                        validateAndIndexResults(
                                currentValidationResults,
                                eventsById);

        List<EventValidationAlert> validatedOpenAlerts =
                validateOpenAlerts(
                        openAlerts,
                        eventsById);

        List<Vr008Issue> issues =
                new ArrayList<>();

        if (events.isEmpty()) {
            issues.add(
                    otherCriticalIssue(
                            null,
                            "The Operational Close contains no "
                                    + "Operational Events."));
        }

        appendEventStateIssues(
                events,
                issues);

        appendValidationResultIssues(
                readiness,
                eventsById,
                resultsByEvent,
                issues);

        appendBlockingAlertIssues(
                readiness,
                validatedOpenAlerts,
                issues);

        appendConsolidationIssues(
                closeId,
                events,
                consolidation,
                issues);

        if (issues.isEmpty()
                && !readiness.ready()) {

            issues.add(
                    otherCriticalIssue(
                            null,
                            "The final readiness evaluation is not "
                                    + "satisfied."));
        }

        if (issues.isEmpty()) {
            return Vr008Evaluation.satisfied(
                    Objects.requireNonNull(
                            consolidation,
                            "satisfied VR-008 evaluation "
                                    + "requires a consolidation")
                            .id());
        }

        return Vr008Evaluation.failed(
                consolidation == null
                        ? null
                        : consolidation.id(),
                issues);
    }

    private static void appendEventStateIssues(
            List<OperationalEvent> events,
            List<Vr008Issue> issues) {

        for (OperationalEvent event : events) {
            if (event.state()
                    != OperationalEventState.VALIDATED) {

                issues.add(
                        new Vr008Issue(
                                SubmissionAttemptIssueType
                                        .EVENT_NOT_VALIDATED,
                                event.id(),
                                null,
                                null,
                                null,
                                "Operational Event "
                                        + event.id()
                                        + " is not in VALIDATED state."));
            }
        }
    }

    private static void appendValidationResultIssues(
            CloseConsolidationReadiness readiness,
            Map<OperationalEventId, OperationalEvent> eventsById,
            Map<OperationalEventId, List<EventValidationResult>>
                    resultsByEvent,
            List<Vr008Issue> issues) {

        for (OperationalEventId eventId
                : readiness.invalidResultEventIds()) {

            OperationalEvent event =
                    eventsById.get(
                            eventId);

            List<EventValidationResult> results =
                    resultsByEvent.getOrDefault(
                            eventId,
                            List.of());

            int previousIssueCount =
                    issues.size();

            for (EventValidationResult result : results) {
                if (result.outcome()
                        == ValidationOutcome.FAILED) {

                    issues.add(
                            validationResultIssue(
                                    SubmissionAttemptIssueType
                                            .VALIDATION_RESULT_FAILED,
                                    eventId,
                                    result.id(),
                                    "Validation Result "
                                            + result.id()
                                            + " is FAILED."));
                }
                else if (!result.current()
                        || result.eventDataRevision()
                                != event.dataRevision()) {

                    issues.add(
                            validationResultIssue(
                                    SubmissionAttemptIssueType
                                            .VALIDATION_RESULT_STALE,
                                    eventId,
                                    result.id(),
                                    "Validation Result "
                                            + result.id()
                                            + " is not applicable to the "
                                            + "current Event revision."));
                }
            }

            if (issues.size()
                    == previousIssueCount) {

                issues.add(
                        otherCriticalIssue(
                                eventId,
                                "Operational Event "
                                        + eventId
                                        + " does not have a complete, "
                                        + "current and satisfied set of "
                                        + "applicable Validation Results."));
            }
        }
    }

    private static void appendBlockingAlertIssues(
            CloseConsolidationReadiness readiness,
            List<EventValidationAlert> openAlerts,
            List<Vr008Issue> issues) {

        Set<OperationalEventId> representedEventIds =
                new HashSet<>();

        for (EventValidationAlert alert : openAlerts) {
            if (!alert.blocking()) {
                continue;
            }

            representedEventIds.add(
                    alert.eventId());

            issues.add(
                    new Vr008Issue(
                            SubmissionAttemptIssueType.BLOCKING_ALERT,
                            alert.eventId(),
                            alert.id(),
                            null,
                            null,
                            "Blocking Validation Alert "
                                    + alert.id()
                                    + " remains open."));
        }

        for (OperationalEventId eventId
                : readiness.blockingAlertEventIds()) {

            if (!representedEventIds.contains(
                    eventId)) {

                issues.add(
                        otherCriticalIssue(
                                eventId,
                                "The readiness evaluation reports a "
                                        + "blocking Alert for Operational "
                                        + "Event "
                                        + eventId
                                        + ", but the Alert was not present "
                                        + "in the reloaded collection."));
            }
        }
    }

    private static void appendConsolidationIssues(
            OperationalCloseId closeId,
            List<OperationalEvent> events,
            Consolidation consolidation,
            List<Vr008Issue> issues) {

        if (consolidation == null) {
            issues.add(
                    new Vr008Issue(
                            SubmissionAttemptIssueType
                                    .CONSOLIDATION_MISSING,
                            null,
                            null,
                            null,
                            null,
                            "The Operational Close does not have a "
                                    + "current Consolidation."));

            return;
        }

        if (!matchesCurrentCloseData(
                closeId,
                events,
                consolidation)) {

            issues.add(
                    new Vr008Issue(
                            SubmissionAttemptIssueType
                                    .CONSOLIDATION_STALE,
                            null,
                            null,
                            null,
                            consolidation.id(),
                            "Consolidation "
                                    + consolidation.id()
                                    + " does not match the current "
                                    + "Operational Close data."));
        }
    }

    private static boolean matchesCurrentCloseData(
            OperationalCloseId closeId,
            List<OperationalEvent> events,
            Consolidation consolidation) {

        if (!consolidation.current()
                || !closeId.equals(
                        consolidation.closeId())
                || consolidation.eventCount()
                        != events.size()) {

            return false;
        }

        Map<OperationalEventId, ConsolidationEventSnapshot>
                snapshotsByEvent =
                        new HashMap<>();

        for (ConsolidationEventSnapshot snapshot
                : consolidation.eventSnapshots()) {

            if (snapshotsByEvent.put(
                    snapshot.eventId(),
                    snapshot) != null) {

                return false;
            }
        }

        if (snapshotsByEvent.size()
                != events.size()) {

            return false;
        }

        for (OperationalEvent event : events) {
            ConsolidationEventSnapshot snapshot =
                    snapshotsByEvent.get(
                            event.id());

            if (snapshot == null
                    || !matchesEvent(
                            snapshot,
                            event)) {

                return false;
            }
        }

        return true;
    }

    private static boolean matchesEvent(
            ConsolidationEventSnapshot snapshot,
            OperationalEvent event) {

        return snapshot.eventDataRevision()
                        == event.dataRevision()
                && snapshot.eventType()
                        == event.eventType()
                && monetaryEquals(
                        snapshot.amount().value(),
                        event.amount().value())
                && monetaryEquals(
                        snapshot.balanceEffect(),
                        event.balanceEffect())
                && Objects.equals(
                        snapshot.reversedEventId(),
                        event.reversedEventId())
                && snapshot.eventState()
                        == event.state();
    }

    private static Map<OperationalEventId, OperationalEvent>
            validateAndIndexEvents(
                    OperationalCloseId closeId,
                    List<OperationalEvent> events) {

        Map<OperationalEventId, OperationalEvent> eventsById =
                new HashMap<>();

        for (OperationalEvent event : events) {
            requireNonNull(
                    event,
                    "event");

            if (!closeId.equals(
                    event.closeId())) {

                throw new IllegalArgumentException(
                        "all Events must belong to the evaluated Close");
            }

            if (eventsById.put(
                    event.id(),
                    event) != null) {

                throw new IllegalArgumentException(
                        "Events must not contain duplicate identifiers");
            }
        }

        return Map.copyOf(
                eventsById);
    }

    private static void validateReadiness(
            CloseConsolidationReadiness readiness,
            Map<OperationalEventId, OperationalEvent> eventsById) {

        if (readiness.eventsPresent()
                != !eventsById.isEmpty()) {

            throw new IllegalArgumentException(
                    "readiness event-presence flag must match "
                            + "the Event collection");
        }

        Set<OperationalEventId> referencedEventIds =
                new HashSet<>();

        referencedEventIds.addAll(
                readiness.notValidatedEventIds());

        referencedEventIds.addAll(
                readiness.invalidResultEventIds());

        referencedEventIds.addAll(
                readiness.blockingAlertEventIds());

        if (!eventsById.keySet()
                .containsAll(
                        referencedEventIds)) {

            throw new IllegalArgumentException(
                    "readiness must reference only evaluated Events");
        }
    }

    private static Map<
            OperationalEventId,
            List<EventValidationResult>>
            validateAndIndexResults(
                    List<EventValidationResult> results,
                    Map<OperationalEventId, OperationalEvent> eventsById) {

        Map<OperationalEventId, List<EventValidationResult>>
                resultsByEvent =
                        new HashMap<>();

        Set<ValidationResultId> resultIds =
                new HashSet<>();

        for (EventValidationResult result : results) {
            requireNonNull(
                    result,
                    "validationResult");

            if (!eventsById.containsKey(
                    result.eventId())) {

                throw new IllegalArgumentException(
                        "Validation Results must belong to "
                                + "evaluated Events");
            }

            if (!resultIds.add(
                    result.id())) {

                throw new IllegalArgumentException(
                        "Validation Results must not contain "
                                + "duplicate identifiers");
            }

            resultsByEvent
                    .computeIfAbsent(
                            result.eventId(),
                            ignored ->
                                    new ArrayList<>())
                    .add(
                            result);
        }

        return resultsByEvent;
    }

    private static List<EventValidationAlert> validateOpenAlerts(
            List<EventValidationAlert> alerts,
            Map<OperationalEventId, OperationalEvent> eventsById) {

        Set<ValidationAlertId> alertIds =
                new HashSet<>();

        for (EventValidationAlert alert : alerts) {
            requireNonNull(
                    alert,
                    "alert");

            if (!eventsById.containsKey(
                    alert.eventId())) {

                throw new IllegalArgumentException(
                        "Validation Alerts must belong to "
                                + "evaluated Events");
            }

            if (alert.state().terminal()) {
                throw new IllegalArgumentException(
                        "open Alert collection must not contain "
                                + "terminal Alerts");
            }

            if (!alertIds.add(
                    alert.id())) {

                throw new IllegalArgumentException(
                        "Validation Alerts must not contain "
                                + "duplicate identifiers");
            }
        }

        return List.copyOf(
                alerts);
    }

    private static Vr008Issue validationResultIssue(
            SubmissionAttemptIssueType issueType,
            OperationalEventId eventId,
            ValidationResultId resultId,
            String detail) {

        return new Vr008Issue(
                issueType,
                eventId,
                null,
                resultId,
                null,
                detail);
    }

    private static Vr008Issue otherCriticalIssue(
            OperationalEventId eventId,
            String detail) {

        return new Vr008Issue(
                SubmissionAttemptIssueType
                        .OTHER_CRITICAL_INCONSISTENCY,
                eventId,
                null,
                null,
                null,
                detail);
    }

    private static boolean monetaryEquals(
            BigDecimal first,
            BigDecimal second) {

        return first.compareTo(
                second) == 0;
    }

    private static void requireNonNull(
            Object value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null");
        }
    }

}