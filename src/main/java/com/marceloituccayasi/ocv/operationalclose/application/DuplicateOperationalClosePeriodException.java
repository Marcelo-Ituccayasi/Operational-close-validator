package com.marceloituccayasi.ocv.operationalclose.application;

/**
 * Signals that another Operational Close already owns the requested period.
 */
public final class DuplicateOperationalClosePeriodException
        extends RuntimeException {

    public DuplicateOperationalClosePeriodException() {
        super("An Operational Close already exists for the requested period.");
    }

    public DuplicateOperationalClosePeriodException(
            Throwable cause) {

        super(
                "An Operational Close already exists for the requested period.",
                cause);
    }

}
