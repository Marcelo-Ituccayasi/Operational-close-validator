package com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persistence representation of the single configured identity user.
 */
@Entity
@Table(name = "identity_user", schema = "ocv")
public class IdentityUserEntity {

    public static final String RESPONSIBLE_USER_ID = "responsible-user";

    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    private String userId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "username_normalized", nullable = false, length = 100)
    private String usernameNormalized;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    @Column(name = "provisioned_at", nullable = false)
    private Instant provisionedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityUserEntity() {
        // Required by JPA.
    }

    private IdentityUserEntity(
            String userId,
            String username,
            String usernameNormalized,
            String passwordHash,
            boolean enabled,
            long credentialVersion,
            Instant provisionedAt,
            Instant updatedAt) {

        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        this.usernameNormalized = Objects.requireNonNull(usernameNormalized);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.enabled = enabled;
        this.credentialVersion = credentialVersion;
        this.provisionedAt = Objects.requireNonNull(provisionedAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static IdentityUserEntity provision(
            String username,
            String usernameNormalized,
            String passwordHash,
            Instant provisionedAt) {

        return new IdentityUserEntity(
                RESPONSIBLE_USER_ID,
                username,
                usernameNormalized,
                passwordHash,
                true,
                1L,
                provisionedAt,
                provisionedAt);
    }

    /**
     * Synchronizes externally configured identity data.
     *
     * @return true when persisted state changed
     */
    public boolean synchronize(
            String configuredUsername,
            String configuredUsernameNormalized,
            String configuredPasswordHash,
            Instant synchronizationTime) {

        Objects.requireNonNull(configuredUsername);
        Objects.requireNonNull(configuredUsernameNormalized);
        Objects.requireNonNull(configuredPasswordHash);
        Objects.requireNonNull(synchronizationTime);

        boolean passwordChanged =
                !passwordHash.equals(configuredPasswordHash);

        boolean changed =
                !username.equals(configuredUsername)
                        || !usernameNormalized.equals(configuredUsernameNormalized)
                        || passwordChanged
                        || !enabled;

        if (!changed) {
            return false;
        }

        username = configuredUsername;
        usernameNormalized = configuredUsernameNormalized;
        enabled = true;

        if (passwordChanged) {
            passwordHash = configuredPasswordHash;
            credentialVersion++;
        }

        updatedAt = synchronizationTime;
        return true;
    }

    public String userId() {
        return userId;
    }

    public String username() {
        return username;
    }

    public String usernameNormalized() {
        return usernameNormalized;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long credentialVersion() {
        return credentialVersion;
    }

    public Instant provisionedAt() {
        return provisionedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

}
