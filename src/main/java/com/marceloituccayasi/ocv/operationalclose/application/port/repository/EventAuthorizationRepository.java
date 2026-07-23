package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorization;
import com.marceloituccayasi.ocv.operationalclose.domain.EventAuthorizationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Persistence contract required by Event Authorization use cases.
 *
 * <p>Mutation use cases must lock the owning Operational Close before
 * invoking {@link #findByIdForUpdate(
 * OperationalCloseId, EventAuthorizationId)}.
 */
public interface EventAuthorizationRepository {

    void saveNew(
            EventAuthorization eventAuthorization);

    Optional<EventAuthorization> findById(
            EventAuthorizationId authorizationId);

    List<EventAuthorization>
            findAllByEventIdOrderByAuthorizedAtDescending(
                    OperationalEventId eventId);

    Optional<EventAuthorization> findByIdForUpdate(
            OperationalCloseId closeId,
            EventAuthorizationId authorizationId);

    void saveRevision(
            EventAuthorization eventAuthorization);

}