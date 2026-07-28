package com.marceloituccayasi.ocv.operationalclose.domain;

/**
 * Lifecycle states approved for a Validation Alert.
 */
public enum ValidationAlertState {

    ACTIVE,
    ACKNOWLEDGED,
    UNDER_REVIEW,
    RESOLVED,
    DISCARDED;

    public boolean terminal() {
        return this == RESOLVED
                || this == DISCARDED;
    }

}