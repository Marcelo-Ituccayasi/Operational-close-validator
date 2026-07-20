package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * External authentication configuration for the single responsible user.
 *
 * <p>Values are supplied through {@code OCV_AUTH_USERNAME} and
 * {@code OCV_AUTH_PASSWORD_HASH}. No production defaults are provided.
 *
 * @param username normalized authentication username
 * @param passwordHash delegating password encoder value using the bcrypt prefix
 */
@Validated
@ConfigurationProperties(prefix = "ocv.auth")
public record AuthenticationProperties(

        @NotBlank(message = "OCV_AUTH_USERNAME must not be blank")
        @Size(
                max = 100,
                message = "OCV_AUTH_USERNAME must not exceed 100 characters"
        )
        String username,

        @NotBlank(message = "OCV_AUTH_PASSWORD_HASH must not be blank")
        @Size(
                max = 255,
                message = "OCV_AUTH_PASSWORD_HASH must not exceed 255 characters"
        )
        @Pattern(
                regexp = "^\\{bcrypt\\}\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$",
                message = "OCV_AUTH_PASSWORD_HASH must use {bcrypt}<encoded-password>"
        )
        String passwordHash) {

    public AuthenticationProperties {
        username = normalizeUsername(username);
        passwordHash = normalizePasswordHash(passwordHash);
    }

    private static String normalizeUsername(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePasswordHash(String value) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }


    @Override
    public String toString() {
        return "AuthenticationProperties[username="
                + username
                + ", passwordHash=<redacted>]";
    }
}
