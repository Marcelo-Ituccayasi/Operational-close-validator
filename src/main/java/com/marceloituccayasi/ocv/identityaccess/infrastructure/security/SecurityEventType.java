package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

/**
 * Stable security event codes persisted by identity and access.
 */
public enum SecurityEventType {

    USER_PROVISIONED,
    CREDENTIAL_ROTATED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    LOGIN_RATE_LIMITED,
    SESSION_REPLACED,
    LOGOUT,
    SESSION_EXPIRED,
    CONFIGURATION_FAILED

}
