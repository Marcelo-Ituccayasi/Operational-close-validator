package com.marceloituccayasi.ocv.operationalclose.domain;

import java.util.regex.Pattern;

/**
 * Three-letter uppercase currency code.
 *
 * @param value persisted currency code
 */
public record CurrencyCode(String value) {

    private static final Pattern VALID_CODE =
            Pattern.compile("[A-Z]{3}");

    public CurrencyCode {
        if (value == null
                || !VALID_CODE.matcher(value).matches()) {

            throw new IllegalArgumentException(
                    "currency code must contain exactly three uppercase letters");
        }
    }

    @Override
    public String toString() {
        return value;
    }

}
