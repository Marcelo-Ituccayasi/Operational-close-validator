package com.marceloituccayasi.ocv.operationalclose.application.port;

import java.util.UUID;

/**
 * Generates application-owned UUID values.
 */
@FunctionalInterface
public interface UuidGenerator {

    UUID next();

}
