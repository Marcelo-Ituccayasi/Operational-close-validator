package com.marceloituccayasi.ocv.identityaccess.application;

/**
 * Framework-independent representation of the authenticated application principal.
 *
 * @param userId stable internal user identifier
 * @param username authenticated username
 */
public record AuthenticatedPrincipal(
        String userId,
        String username) implements AuthenticatedIdentity {

    public AuthenticatedPrincipal {
        requireNonBlank(userId, "userId");
        requireNonBlank(username, "username");
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }
    }

}
