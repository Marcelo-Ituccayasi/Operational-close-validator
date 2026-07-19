package com.marceloituccayasi.ocv.operationalclose.application.port;

import java.time.Instant;

/**
 * Supplies the current application time without coupling use cases to the system clock.
 */
@FunctionalInterface
public interface ApplicationClock {

    Instant now();

}
