package com.marceloituccayasi.ocv.operationalclose.infrastructure.id;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.application.port.UuidGenerator;

/**
 * Generates application-owned random UUID values.
 */
@Component
public final class RandomUuidGenerator implements UuidGenerator {

    @Override
    public UUID next() {
        return UUID.randomUUID();
    }

}
