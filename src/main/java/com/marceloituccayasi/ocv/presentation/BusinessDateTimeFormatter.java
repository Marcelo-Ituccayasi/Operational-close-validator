package com.marceloituccayasi.ocv.presentation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Parses and formats instants at the configured business presentation boundary.
 */
public final class BusinessDateTimeFormatter {

    private static final DateTimeFormatter INPUT_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "uuuu-MM-dd'T'HH:mm:ss",
                    Locale.ROOT);

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "uuuu-MM-dd HH:mm:ss XXX '['VV']'",
                    Locale.ROOT);

    private final ZoneId zoneId;

    public BusinessDateTimeFormatter(
            ZoneId zoneId) {

        this.zoneId =
                Objects.requireNonNull(
                        zoneId,
                        "zoneId must not be null");
    }

    /**
     * Parses either an ISO date-time with an explicit offset or a local
     * date-time interpreted in the configured business time zone.
     *
     * @param value submitted date-time text
     * @return corresponding UTC-compatible instant
     */
    public Instant parse(
            String value) {

        String normalizedValue =
                requireValue(
                        value);

        try {
            return OffsetDateTime.parse(
                    normalizedValue,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant();
        }
        catch (DateTimeParseException exception) {
            // Fall through to local business date-time parsing.
        }

        LocalDateTime localDateTime =
                LocalDateTime.parse(
                        normalizedValue,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        List<ZoneOffset> validOffsets =
                zoneId.getRules()
                        .getValidOffsets(
                                localDateTime);

        if (validOffsets.size() != 1) {
            throw new DateTimeParseException(
                    "Local date-time is invalid or ambiguous "
                            + "in OCV_BUSINESS_TIME_ZONE",
                    normalizedValue,
                    0);
        }

        return localDateTime.toInstant(
                validOffsets.get(0));
    }

    /**
     * Formats an instant for an editable local date-time field.
     *
     * @param instant stored instant
     * @return local business date-time text
     */
    public String formatForInput(
            Instant instant) {

        return INPUT_FORMATTER.format(
                requireInstant(
                        instant)
                        .atZone(
                                zoneId));
    }

    /**
     * Formats an instant for display with offset and zone identifier.
     *
     * @param instant stored instant
     * @return business-zone display value
     */
    public String format(
            Instant instant) {

        return DISPLAY_FORMATTER.format(
                requireInstant(
                        instant)
                        .atZone(
                                zoneId));
    }

    public String zoneId() {
        return zoneId.getId();
    }

    private static String requireValue(
            String value) {

        if (value == null || value.isBlank()) {
            throw new DateTimeParseException(
                    "Date-time value must not be blank",
                    value == null
                            ? ""
                            : value,
                    0);
        }

        return value.trim();
    }

    private static Instant requireInstant(
            Instant instant) {

        return Objects.requireNonNull(
                instant,
                "instant must not be null");
    }

}