package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Persistence contract for revising Operational Events under
 * pessimistic database locking.
 *
 * Every lock is scoped to the Operational Close that was locked first by the
 * application transaction.
 */
public interface OperationalEventRevisionRepository {

    Optional<OperationalEvent> findByIdForUpdate(
            OperationalCloseId closeId,
            OperationalEventId eventId);

    Optional<OperationalEvent>
            findCancellationByReversedEventIdForUpdate(
                    OperationalCloseId closeId,
                    OperationalEventId reversedEventId);

    void saveRevision(
            OperationalEvent operationalEvent);

    void appendStateTransition(
            EventStateTransition stateTransition);

}