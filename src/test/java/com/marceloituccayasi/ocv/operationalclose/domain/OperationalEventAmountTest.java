package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OperationalEventAmountTest {

    @Test
    void acceptsPositiveAmountWithFourDecimalPlaces() {
        OperationalEventAmount amount =
                new OperationalEventAmount(
                        new BigDecimal("125.5000"));

        assertThat(amount.value())
                .isEqualByComparingTo("125.5000");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(
                () -> new OperationalEventAmount(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(
                () -> new OperationalEventAmount(
                        BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(
                () -> new OperationalEventAmount(
                        new BigDecimal("-0.0001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    void rejectsMoreThanFourDecimalPlaces() {
        assertThatThrownBy(
                () -> new OperationalEventAmount(
                        new BigDecimal("10.00001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("four decimal places");
    }

    @Test
    void rejectsMoreThanFifteenIntegerDigits() {
        assertThatThrownBy(
                () -> new OperationalEventAmount(
                        new BigDecimal(
                                "1000000000000000.0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numeric(19,4)");
    }

}