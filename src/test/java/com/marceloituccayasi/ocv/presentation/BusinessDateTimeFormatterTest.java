package com.marceloituccayasi.ocv.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class BusinessDateTimeFormatterTest {

    @Test
    void parsesLocalDateTimeInConfiguredBusinessTimeZone() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "America/Lima"));

        Instant parsed =
                formatter.parse(
                        "2026-07-22T10:30:00");

        assertThat(parsed)
                .isEqualTo(
                        Instant.parse(
                                "2026-07-22T15:30:00Z"));
    }

    @Test
    void preservesCompatibilityWithExplicitOffsetInput() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "America/Lima"));

        Instant parsed =
                formatter.parse(
                        "2026-07-22T10:30:00-05:00");

        assertThat(parsed)
                .isEqualTo(
                        Instant.parse(
                                "2026-07-22T15:30:00Z"));
    }

    @Test
    void preservesCompatibilityWithUtcInput() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "America/Lima"));

        Instant parsed =
                formatter.parse(
                        "2026-07-22T15:30:00Z");

        assertThat(parsed)
                .isEqualTo(
                        Instant.parse(
                                "2026-07-22T15:30:00Z"));
    }

    @Test
    void formatsEditableValueInBusinessTimeZone() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "America/Lima"));

        assertThat(
                formatter.formatForInput(
                        Instant.parse(
                                "2026-07-22T15:30:00Z")))
                .isEqualTo(
                        "2026-07-22T10:30:00");
    }

    @Test
    void formatsDisplayValueWithOffsetAndZoneIdentifier() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "America/Lima"));

        assertThat(
                formatter.format(
                        Instant.parse(
                                "2026-07-22T15:30:00Z")))
                .isEqualTo(
                        "2026-07-22 10:30:00 -05:00 [America/Lima]");
    }

    @Test
    void rejectsNonexistentLocalDateTimeDuringDaylightSavingGap() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "Europe/Berlin"));

        assertThatThrownBy(
                () -> formatter.parse(
                        "2026-03-29T02:30:00"))
                .isInstanceOf(
                        DateTimeParseException.class);
    }

    @Test
    void rejectsAmbiguousLocalDateTimeDuringDaylightSavingOverlap() {
        BusinessDateTimeFormatter formatter =
                new BusinessDateTimeFormatter(
                        ZoneId.of(
                                "Europe/Berlin"));

        assertThatThrownBy(
                () -> formatter.parse(
                        "2026-10-25T02:30:00"))
                .isInstanceOf(
                        DateTimeParseException.class);
    }

}