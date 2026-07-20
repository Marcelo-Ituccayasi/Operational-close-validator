package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AuthenticationPropertiesTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesValidExternalConfiguration() {
        AuthenticationProperties properties =
                new AuthenticationProperties(
                        "  Responsible  ",
                        "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

        Set<ConstraintViolation<AuthenticationProperties>> violations =
                validator.validate(properties);

        assertThat(violations).isEmpty();
        assertThat(properties.username()).isEqualTo("responsible");
        assertThat(properties.passwordHash()).startsWith("{bcrypt}$2a$10$");
    }

    @Test
    void rejectsBlankUsername() {
        AuthenticationProperties properties =
                new AuthenticationProperties(
                        " ",
                        "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

        Set<ConstraintViolation<AuthenticationProperties>> violations =
                validator.validate(properties);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("OCV_AUTH_USERNAME must not be blank");
    }

    @Test
    void rejectsPasswordWithoutBcryptPrefix() {
        AuthenticationProperties properties =
                new AuthenticationProperties(
                        "responsible",
                        "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

        Set<ConstraintViolation<AuthenticationProperties>> violations =
                validator.validate(properties);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "OCV_AUTH_PASSWORD_HASH must use {bcrypt}<encoded-password>");
    }


    @Test
    void masksPasswordHashInStringRepresentation() {
        String passwordHash =
                "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMye"
                        + "IjZAgcfl7p92ldGxad68LJZdL17lhWy";

        AuthenticationProperties properties =
                new AuthenticationProperties(
                        "responsible",
                        passwordHash);

        assertThat(properties.toString())
                .contains(
                        "responsible",
                        "passwordHash=<redacted>")
                .doesNotContain(
                        passwordHash,
                        "$2a$");
    }
}
