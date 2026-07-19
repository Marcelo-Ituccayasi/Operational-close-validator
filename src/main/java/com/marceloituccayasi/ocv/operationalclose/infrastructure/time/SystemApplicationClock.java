package com.marceloituccayasi.ocv.operationalclose.infrastructure.time;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.application.port.ApplicationClock;

/**
 * Supplies UTC-compatible instants from the system clock.
 */
@Component
public final class SystemApplicationClock implements ApplicationClock {

    @Override
    public Instant now() {
        return Instant.now();
    }

}
