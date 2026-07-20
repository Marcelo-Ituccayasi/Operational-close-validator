package com.marceloituccayasi.ocv.operationalclose.domain;

import java.time.LocalDate;

/**
 * Inclusive business period covered by an Operational Close.
 *
 * @param startDate first date of the period
 * @param endDate last date of the period
 */
public record OperationalPeriod(
        LocalDate startDate,
        LocalDate endDate) {

    public OperationalPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException(
                    "period start date must not be null");
        }

        if (endDate == null) {
            throw new IllegalArgumentException(
                    "period end date must not be null");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "period end date must not be before start date");
        }
    }

}
