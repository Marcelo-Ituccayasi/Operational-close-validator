package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Explicit application result for Operational Close consolidation.
 *
 * @param status operation status
 * @param consolidationId created consolidation when successful
 * @param affectedEventIds Events requiring correction after a business
 *        rejection
 * @param message safe result description when applicable
 */
public record CompleteOperationalCloseConsolidationResult(
        Status status,
        UUID consolidationId,
        List<UUID> affectedEventIds,
        String message) {

    public enum Status {
        CONSOLIDATED,
        CONSOLIDATION_REJECTED,
        INVALID_INPUT,
        ACTOR_REJECTED,
        CLOSE_NOT_FOUND,
        CLOSE_NOT_CONSOLIDATABLE
    }

    public CompleteOperationalCloseConsolidationResult {
        Objects.requireNonNull(
                status,
                "status must not be null");

        Objects.requireNonNull(
                affectedEventIds,
                "affectedEventIds must not be null");

        affectedEventIds =
                affectedEventIds.stream()
                        .map(
                                eventId ->
                                        Objects.requireNonNull(
                                                eventId,
                                                "affectedEventIds must not "
                                                        + "contain null values"))
                        .distinct()
                        .sorted(
                                Comparator.naturalOrder())
                        .toList();

        if (status == Status.CONSOLIDATED) {
            Objects.requireNonNull(
                    consolidationId,
                    "consolidated result must contain consolidationId");

            if (!affectedEventIds.isEmpty()
                    || message != null) {

                throw new IllegalArgumentException(
                        "consolidated result must not contain "
                                + "rejection information");
            }
        }
        else {
            if (consolidationId != null) {
                throw new IllegalArgumentException(
                        "non-consolidated result must not contain "
                                + "consolidationId");
            }

            if (message == null
                    || message.isBlank()) {

                throw new IllegalArgumentException(
                        "non-consolidated result must contain message");
            }

            if (status
                    != Status.CONSOLIDATION_REJECTED
                    && !affectedEventIds.isEmpty()) {

                throw new IllegalArgumentException(
                        "only a business rejection may contain "
                                + "affected Event identifiers");
            }
        }
    }

    public static CompleteOperationalCloseConsolidationResult
            consolidated(
                    UUID consolidationId) {

        return new CompleteOperationalCloseConsolidationResult(
                Status.CONSOLIDATED,
                Objects.requireNonNull(
                        consolidationId),
                List.of(),
                null);
    }

    public static CompleteOperationalCloseConsolidationResult
            rejected(
                    List<UUID> affectedEventIds,
                    String message) {

        return new CompleteOperationalCloseConsolidationResult(
                Status.CONSOLIDATION_REJECTED,
                null,
                affectedEventIds,
                Objects.requireNonNull(
                        message));
    }

    public static CompleteOperationalCloseConsolidationResult
            invalidInput(
                    String message) {

        return unsuccessful(
                Status.INVALID_INPUT,
                message);
    }

    public static CompleteOperationalCloseConsolidationResult
            actorRejected() {

        return unsuccessful(
                Status.ACTOR_REJECTED,
                "The authenticated actor cannot perform this operation.");
    }

    public static CompleteOperationalCloseConsolidationResult
            closeNotFound() {

        return unsuccessful(
                Status.CLOSE_NOT_FOUND,
                "The requested Operational Close does not exist.");
    }

    public static CompleteOperationalCloseConsolidationResult
            closeNotConsolidatable() {

        return unsuccessful(
                Status.CLOSE_NOT_CONSOLIDATABLE,
                "The Operational Close must be in preparation or "
                        + "blocked state.");
    }

    private static CompleteOperationalCloseConsolidationResult
            unsuccessful(
                    Status status,
                    String message) {

        return new CompleteOperationalCloseConsolidationResult(
                status,
                null,
                List.of(),
                Objects.requireNonNull(
                        message));
    }

}