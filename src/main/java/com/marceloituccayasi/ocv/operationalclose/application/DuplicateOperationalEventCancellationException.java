package com.marceloituccayasi.ocv.operationalclose.application;

/**
 * Signals that an Operational Event already has a persisted cancellation.
 */
public final class DuplicateOperationalEventCancellationException
        extends RuntimeException {

    public DuplicateOperationalEventCancellationException() {
        super();
    }

    public DuplicateOperationalEventCancellationException(
            Throwable cause) {

        super(cause);
    }

}