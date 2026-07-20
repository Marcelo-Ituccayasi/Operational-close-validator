package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;

/**
 * Framework-independent application view of an Operational Close.
 */
public record OperationalCloseView(
        UUID id,
        LocalDate periodStart,
        LocalDate periodEnd,
        String currencyCode,
        BigDecimal initialBalance,
        String state,
        Instant stateChangedAt,
        Instant createdAt,
        String createdByUserId,
        String createdByUsername,
        Instant updatedAt,
        String updatedByUserId,
        String updatedByUsername) {

    static OperationalCloseView fromDomain(
            OperationalClose operationalClose) {

        return new OperationalCloseView(
                operationalClose.id().value(),
                operationalClose.period().startDate(),
                operationalClose.period().endDate(),
                operationalClose.currencyCode().value(),
                operationalClose.initialBalance().value(),
                operationalClose.state().name(),
                operationalClose.stateChangedAt(),
                operationalClose.createdAt(),
                operationalClose.createdBy().userId(),
                operationalClose.createdBy().username(),
                operationalClose.updatedAt(),
                operationalClose.updatedBy().userId(),
                operationalClose.updatedBy().username());
    }

}
