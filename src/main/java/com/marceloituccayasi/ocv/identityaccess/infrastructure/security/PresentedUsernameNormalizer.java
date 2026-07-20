package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.util.Locale;

/**
 * Normalizes untrusted usernames submitted through the login form.
 */
public final class PresentedUsernameNormalizer {

    public static final int MAXIMUM_LENGTH = 100;

    private PresentedUsernameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized =
                value.trim().toLowerCase(Locale.ROOT);

        if (normalized.length()
                <= MAXIMUM_LENGTH) {

            return normalized;
        }

        return normalized.substring(
                0,
                MAXIMUM_LENGTH);
    }

}
