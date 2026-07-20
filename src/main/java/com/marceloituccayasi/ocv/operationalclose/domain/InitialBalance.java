package com.marceloituccayasi.ocv.operationalclose.domain;

import java.math.BigDecimal;

/**
 * Non-negative initial balance of an Operational Close.
 *
 * @param value exact decimal value
 */
public record InitialBalance(BigDecimal value) {

    private static final int MAXIMUM_SCALE = 4;
    private static final int MAXIMUM_INTEGER_DIGITS = 15;

    public InitialBalance {
        if (value == null) {
            throw new IllegalArgumentException(
                    "initial balance must not be null");
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    "initial balance must not be negative");
        }

        if (value.scale() > MAXIMUM_SCALE) {
            throw new IllegalArgumentException(
                    "initial balance must not exceed four decimal places");
        }

        int integerDigits =
                value.precision() - value.scale();

        if (integerDigits > MAXIMUM_INTEGER_DIGITS) {
            throw new IllegalArgumentException(
                    "initial balance exceeds numeric(19,4)");
        }
    }

}
