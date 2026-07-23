package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;

/**
 * Framework-independent application view of an Operational Event.
 */
public record OperationalEventView(
        UUID id,
        UUID closeId,
        String eventType,
        BigDecimal amount,
        BigDecimal balanceEffect,
        UUID reversedEventId,
        Instant occurredAt,
        Instant registeredAt,
        String responsibleName,
        String description,
        String state,
        boolean evidenceRequired,
        boolean authorizationRequired,
        long dataRevision,
        Instant stateChangedAt,
        Instant createdAt,
        String createdByUserId,
        String createdByUsername,
        Instant updatedAt,
        String updatedByUserId,
        String updatedByUsername) {

    static OperationalEventView fromDomain(
            OperationalEvent operationalEvent) {

        UUID reversedEventId =
                operationalEvent.reversedEventId() == null
                        ? null
                        : operationalEvent
                                .reversedEventId()
                                .value();

        return new OperationalEventView(
                operationalEvent.id().value(),
                operationalEvent.closeId().value(),
                operationalEvent.eventType().name(),
                operationalEvent.amount().value(),
                operationalEvent.balanceEffect(),
                reversedEventId,
                operationalEvent.occurredAt(),
                operationalEvent.registeredAt(),
                operationalEvent.responsibleName(),
                operationalEvent.description(),
                operationalEvent.state().name(),
                operationalEvent.evidenceRequired(),
                operationalEvent.authorizationRequired(),
                operationalEvent.dataRevision(),
                operationalEvent.stateChangedAt(),
                operationalEvent.createdAt(),
                operationalEvent.createdBy().userId(),
                operationalEvent.createdBy().username(),
                operationalEvent.updatedAt(),
                operationalEvent.updatedBy().userId(),
                operationalEvent.updatedBy().username());
    }

}