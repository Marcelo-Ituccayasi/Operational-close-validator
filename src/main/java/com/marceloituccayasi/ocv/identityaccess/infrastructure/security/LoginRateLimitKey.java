package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.util.Objects;

/**
 * Login rate limit key composed of trusted address and normalized username.
 */
public record LoginRateLimitKey(
        String sourceAddress,
        String usernameNormalized) {

    public LoginRateLimitKey {
        requireNonBlank(
                sourceAddress,
                "sourceAddress");

        Objects.requireNonNull(
                usernameNormalized,
                "usernameNormalized");
    }

    private static void requireNonBlank(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }
    }

}
