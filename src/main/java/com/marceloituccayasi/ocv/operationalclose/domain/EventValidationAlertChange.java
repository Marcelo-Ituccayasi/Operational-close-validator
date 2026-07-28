package com.marceloituccayasi.ocv.operationalclose.domain;

/**
 * Atomic domain result of changing a Validation Alert state.
 *
 * @param alert updated alert
 * @param transition append-only history entry
 */
public record EventValidationAlertChange(
        EventValidationAlert alert,
        ValidationAlertTransition transition) {

    public EventValidationAlertChange {
        if (alert == null) {
            throw new IllegalArgumentException(
                    "updated validation alert must not be null");
        }

        if (transition == null) {
            throw new IllegalArgumentException(
                    "validation alert transition must not be null");
        }

        if (!alert.id().equals(
                transition.alertId())) {

            throw new IllegalArgumentException(
                    "validation alert transition must belong to updated alert");
        }

        if (alert.state()
                != transition.toState()) {

            throw new IllegalArgumentException(
                    "validation alert transition state must match updated alert");
        }

        if (!alert.updatedAt().equals(
                transition.occurredAt())) {

            throw new IllegalArgumentException(
                    "validation alert transition instant must match alert update");
        }
    }

}