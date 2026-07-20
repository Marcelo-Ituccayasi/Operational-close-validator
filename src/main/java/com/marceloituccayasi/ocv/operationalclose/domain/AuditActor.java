package com.marceloituccayasi.ocv.operationalclose.domain;

/**
 * Stable authenticated actor recorded in operational audit data.
 *
 * @param userId stable responsible-user identifier
 * @param username authenticated username
 */
public record AuditActor(
        String userId,
        String username) {

    public static final String RESPONSIBLE_USER_ID =
            "responsible-user";

    public AuditActor {
        if (!RESPONSIBLE_USER_ID.equals(userId)) {
            throw new IllegalArgumentException(
                    "userId must identify the responsible user");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "username must not be blank");
        }

        if (username.length() > 100) {
            throw new IllegalArgumentException(
                    "username must not exceed 100 characters");
        }
    }

}
