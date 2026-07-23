package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Input data required to revise an Operational Event.
 *
 * @param closeId owning Operational Close
 * @param eventId Operational Event being revised
 * @param eventType revised event type
 * @param amount revised positive nominal amount
 * @param reversedEventId referenced event when the revised type is cancellation
 * @param occurredAt revised business occurrence instant
 * @param responsibleName revised business responsible person
 * @param description revised event description
 */
public record UpdateOperationalEventCommand(
        UUID closeId,
        UUID eventId,
        String eventType,
        BigDecimal amount,
        UUID reversedEventId,
        Instant occurredAt,
        String responsibleName,
        String description) {
}