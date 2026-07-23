package com.marceloituccayasi.ocv.operationalclose.application.port;

import com.marceloituccayasi.ocv.operationalclose.application.OperationalEventRequirements;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;

/**
 * Determines the evidence and authorization applicability indicators for an
 * Operational Event.
 */
@FunctionalInterface
public interface OperationalEventRequirementPolicy {

    OperationalEventRequirements determine(
            OperationalEventType eventType,
            OperationalEventAmount amount);

}