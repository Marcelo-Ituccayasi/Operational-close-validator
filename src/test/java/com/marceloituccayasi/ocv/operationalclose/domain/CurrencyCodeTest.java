package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CurrencyCodeTest {

    @Test
    void acceptsThreeUppercaseLetters() {
        CurrencyCode currencyCode =
                new CurrencyCode("PEN");

        assertThat(currencyCode.value())
                .isEqualTo("PEN");
    }

    @Test
    void rejectsLowercaseCode() {
        assertThatThrownBy(
                () -> new CurrencyCode("pen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "three uppercase letters");
    }

    @Test
    void rejectsCodeWithWhitespace() {
        assertThatThrownBy(
                () -> new CurrencyCode(" PEN "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "three uppercase letters");
    }

}
