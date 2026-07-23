package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.EventStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Persistence contract required by Operational Event use cases.
 */
public interface OperationalEventRepository {

    void saveNew(
            OperationalEvent operationalEvent,
            EventStateTransition initialTransition);

    Optional<OperationalEvent> findById(
            OperationalEventId eventId);

    List<OperationalEvent>
            findAllByCloseIdOrderByOccurredAtDescending(
                    OperationalCloseId closeId);

    boolean existsCancellationFor(
            OperationalEventId reversedEventId);

}