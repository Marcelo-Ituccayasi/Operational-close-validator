package com.marceloituccayasi.ocv.operationalclose.infrastructure.policy;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.application.OperationalEventRequirements;
import com.marceloituccayasi.ocv.operationalclose.application.port.OperationalEventRequirementPolicy;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventAmount;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventType;

/**
 * Defers definitive evidence and authorization applicability rules to the
 * implementation increment that introduces those policies.
 */
@Component
public final class DeferredOperationalEventRequirementPolicy
        implements OperationalEventRequirementPolicy {

    private static final OperationalEventRequirements
            DEFERRED_REQUIREMENTS =
                    new OperationalEventRequirements(
                            false,
                            false);

    @Override
    public OperationalEventRequirements determine(
            OperationalEventType eventType,
            OperationalEventAmount amount) {

        Objects.requireNonNull(
                eventType,
                "eventType must not be null");

        Objects.requireNonNull(
                amount,
                "amount must not be null");

        return DEFERRED_REQUIREMENTS;
    }

}