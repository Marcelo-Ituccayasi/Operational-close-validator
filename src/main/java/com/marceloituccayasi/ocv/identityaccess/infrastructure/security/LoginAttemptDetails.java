package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.util.Objects;

/**
 * Trusted request data attached to a form-login authentication attempt.
 */
public record LoginAttemptDetails(
        String sourceAddress,
        String usernameNormalized) {

    public LoginAttemptDetails {
        if (sourceAddress == null
                || sourceAddress.isBlank()) {

            throw new IllegalArgumentException(
                    "sourceAddress must not be blank");
        }

        Objects.requireNonNull(
                usernameNormalized,
                "usernameNormalized");
    }

    public LoginRateLimitKey rateLimitKey() {
        return new LoginRateLimitKey(
                sourceAddress,
                usernameNormalized);
    }

}
