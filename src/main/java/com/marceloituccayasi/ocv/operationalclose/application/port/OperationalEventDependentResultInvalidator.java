package com.marceloituccayasi.ocv.operationalclose.application.port;

import java.util.List;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;

/**
 * Invalidates validation results, consolidation data or other derived
 * information affected by Operational Event revisions.
 */
@FunctionalInterface
public interface OperationalEventDependentResultInvalidator {

    void invalidateForRevisions(
            OperationalCloseId closeId,
            List<OperationalEventId> revisedEventIds);

}