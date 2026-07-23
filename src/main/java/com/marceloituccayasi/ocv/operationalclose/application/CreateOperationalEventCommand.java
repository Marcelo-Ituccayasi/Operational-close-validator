package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Input data required to create an Operational Event.
 *
 * @param closeId owning Operational Close
 * @param eventType requested event type
 * @param amount positive nominal amount
 * @param reversedEventId referenced event for a cancellation
 * @param occurredAt business occurrence instant
 * @param responsibleName business responsible person
 * @param description event description
 */
public record CreateOperationalEventCommand(
        UUID closeId,
        String eventType,
        BigDecimal amount,
        UUID reversedEventId,
        Instant occurredAt,
        String responsibleName,
        String description) {
}