package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InitialBalanceTest {

    @Test
    void acceptsZeroAndFourDecimalPlaces() {
        InitialBalance zero =
                new InitialBalance(
                        new BigDecimal("0.0000"));

        InitialBalance amount =
                new InitialBalance(
                        new BigDecimal("1500.1250"));

        assertThat(zero.value())
                .isEqualByComparingTo("0.0000");

        assertThat(amount.value())
                .isEqualByComparingTo("1500.1250");
    }

    @Test
    void rejectsNegativeValue() {
        assertThatThrownBy(
                () -> new InitialBalance(
                        new BigDecimal("-0.0001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "must not be negative");
    }

    @Test
    void rejectsMoreThanFourDecimalPlaces() {
        assertThatThrownBy(
                () -> new InitialBalance(
                        new BigDecimal("10.00001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "four decimal places");
    }

    @Test
    void rejectsMoreThanFifteenIntegerDigits() {
        assertThatThrownBy(
                () -> new InitialBalance(
                        new BigDecimal(
                                "1000000000000000.0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "numeric(19,4)");
    }

}
