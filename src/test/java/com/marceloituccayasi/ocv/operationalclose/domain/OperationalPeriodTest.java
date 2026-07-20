package com.marceloituccayasi.ocv.operationalclose.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class OperationalPeriodTest {

    @Test
    void acceptsValidPeriod() {
        LocalDate startDate =
                LocalDate.of(2026, 7, 1);

        LocalDate endDate =
                LocalDate.of(2026, 7, 31);

        OperationalPeriod period =
                new OperationalPeriod(
                        startDate,
                        endDate);

        assertThat(period.startDate())
                .isEqualTo(startDate);

        assertThat(period.endDate())
                .isEqualTo(endDate);
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(
                () -> new OperationalPeriod(
                        LocalDate.of(2026, 7, 31),
                        LocalDate.of(2026, 7, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "must not be before");
    }

    @Test
    void rejectsMissingBoundary() {
        assertThatThrownBy(
                () -> new OperationalPeriod(
                        null,
                        LocalDate.of(2026, 7, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "start date");
    }

}
