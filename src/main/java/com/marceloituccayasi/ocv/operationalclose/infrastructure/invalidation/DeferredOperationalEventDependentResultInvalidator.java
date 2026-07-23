package com.marceloituccayasi.ocv.operationalclose.infrastructure.invalidation;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Preparatory invalidation adapter for IP-04.
 *
 * Validation results and consolidated balances are introduced by later
 * implementation increments. Until those persistence structures exist,
 * this adapter validates the invalidation request without producing
 * speculative database records.
 */
@Component
public final class DeferredOperationalEventDependentResultInvalidator
        implements OperationalEventDependentResultInvalidator {

    @Override
    public void invalidateForRevisions(
            OperationalCloseId closeId,
            List<OperationalEventId> revisedEventIds) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                revisedEventIds,
                "revisedEventIds must not be null");

        if (revisedEventIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "revisedEventIds must not be empty");
        }

        revisedEventIds.forEach(
                eventId -> Objects.requireNonNull(
                        eventId,
                        "revisedEventIds must not contain null values"));
    }

}