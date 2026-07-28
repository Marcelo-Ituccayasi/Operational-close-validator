package com.marceloituccayasi.ocv.operationalclose.domain;

import java.util.List;
import java.util.Objects;

/**
 * Derives the resulting Operational Event state from its current rule
 * evaluations.
 */
public final class EventValidationStateResolver {

    public OperationalEventState resolve(
            List<EventValidationRuleEvaluation> evaluations) {

        Objects.requireNonNull(
                evaluations,
                "validation evaluations must not be null");

        boolean supportFailure =
                false;

        boolean authorizationFailure =
                false;

        boolean observationFailure =
                false;

        for (EventValidationRuleEvaluation evaluation
                : evaluations) {

            Objects.requireNonNull(
                    evaluation,
                    "validation evaluation must not be null");

            if (!evaluation.failed()) {
                continue;
            }

            switch (evaluation.ruleCode()) {
                case VR_003 ->
                        supportFailure =
                                true;

                case VR_001, VR_006 ->
                        authorizationFailure =
                                true;

                case VR_002 ->
                        observationFailure =
                                true;

                case VR_008 ->
                        throw new IllegalArgumentException(
                                "close-scoped validation rule cannot determine event state");
            }
        }

        if (supportFailure) {
            return OperationalEventState
                    .PENDING_SUPPORT;
        }

        if (authorizationFailure) {
            return OperationalEventState
                    .PENDING_AUTHORIZATION;
        }

        if (observationFailure) {
            return OperationalEventState
                    .OBSERVED;
        }

        return OperationalEventState
                .VALIDATED;
    }

    public static OperationalEventState
            enforceOpenBlockingAlertInvariant(
                    OperationalEventState resolvedState,
                    boolean blockingAlertOpen) {

        Objects.requireNonNull(
                resolvedState,
                "resolved event state must not be null");

        if (blockingAlertOpen
                && resolvedState
                        == OperationalEventState.VALIDATED) {

            return OperationalEventState
                    .OBSERVED;
        }

        return resolvedState;
    }

}