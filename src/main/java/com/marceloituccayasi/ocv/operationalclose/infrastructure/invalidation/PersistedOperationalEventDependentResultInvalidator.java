package com.marceloituccayasi.ocv.operationalclose.infrastructure.invalidation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventDependentResultInvalidator;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.EventValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.EventValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Invalidates current Event Validation Results affected by relevant
 * Operational Event revisions.
 *
 * <p>Validation Alerts remain unchanged. A later satisfactory revalidation
 * is required to resolve an open Alert.</p>
 */
@Component
public class PersistedOperationalEventDependentResultInvalidator
        implements OperationalEventDependentResultInvalidator {

    static final String EVENT_DATA_REVISION_CHANGED =
            "Operational Event data revision changed.";

    private final EventValidationResultRepository
            validationResultRepository;

    private final ApplicationClock applicationClock;

    public PersistedOperationalEventDependentResultInvalidator(
            EventValidationResultRepository
                    validationResultRepository,
            ApplicationClock applicationClock) {

        this.validationResultRepository =
                Objects.requireNonNull(
                        validationResultRepository);

        this.applicationClock =
                Objects.requireNonNull(
                        applicationClock);
    }

    @Override
    @Transactional
    public void invalidateForRevisions(
            OperationalCloseId closeId,
            List<OperationalEventId> revisedEventIds) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        List<OperationalEventId> normalizedEventIds =
                normalizeEventIds(
                        revisedEventIds);

        Instant invalidatedAt =
                Objects.requireNonNull(
                        applicationClock.now(),
                        "application clock must not return null");

        List<EventValidationResult> currentResults =
                validationResultRepository
                        .findAllCurrentForInvalidation(
                                closeId,
                                normalizedEventIds);

        currentResults.stream()
                .map(
                        result -> result.invalidate(
                                invalidatedAt,
                                EVENT_DATA_REVISION_CHANGED))
                .forEach(
                        validationResultRepository::saveInvalidation);
    }

    private static List<OperationalEventId> normalizeEventIds(
            List<OperationalEventId> revisedEventIds) {

        Objects.requireNonNull(
                revisedEventIds,
                "revisedEventIds must not be null");

        if (revisedEventIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "revisedEventIds must not be empty");
        }

        return revisedEventIds.stream()
                .map(
                        eventId -> Objects.requireNonNull(
                                eventId,
                                "revisedEventIds must not contain null values"))
                .distinct()
                .toList();
    }

}