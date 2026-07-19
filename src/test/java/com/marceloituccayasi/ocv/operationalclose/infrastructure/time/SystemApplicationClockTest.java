package com.marceloituccayasi.ocv.operationalclose.infrastructure.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SystemApplicationClockTest {

    @Test
    void returnsCurrentSystemInstant() {
        SystemApplicationClock clock = new SystemApplicationClock();

        Instant before = Instant.now();
        Instant actual = clock.now();
        Instant after = Instant.now();

        assertThat(actual)
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

}
