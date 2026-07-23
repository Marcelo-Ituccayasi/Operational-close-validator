package com.marceloituccayasi.ocv.operationalclose.domain;

import java.math.BigDecimal;

/**
 * Positive nominal amount of an Operational Event.
 *
 * @param value exact decimal value
 */
public record OperationalEventAmount(BigDecimal value) {

    private static final int MAXIMUM_SCALE = 4;
    private static final int MAXIMUM_INTEGER_DIGITS = 15;

    public OperationalEventAmount {
        if (value == null) {
            throw new IllegalArgumentException(
                    "operational event amount must not be null");
        }

        if (value.signum() <= 0) {
            throw new IllegalArgumentException(
                    "operational event amount must be positive");
        }

        if (value.scale() > MAXIMUM_SCALE) {
            throw new IllegalArgumentException(
                    "operational event amount must not exceed "
                            + "four decimal places");
        }

        int integerDigits =
                value.precision() - value.scale();

        if (integerDigits > MAXIMUM_INTEGER_DIGITS) {
            throw new IllegalArgumentException(
                    "operational event amount exceeds numeric(19,4)");
        }
    }

}