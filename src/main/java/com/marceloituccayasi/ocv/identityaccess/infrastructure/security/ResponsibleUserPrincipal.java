package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;

/**
 * Spring Security principal for the single responsible user.
 *
 * <p>Equality is stable and based only on the internal user identifier.
 */
public final class ResponsibleUserPrincipal
        implements UserDetails, AuthenticatedIdentity {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final long credentialVersion;

    public ResponsibleUserPrincipal(
            String userId,
            String username,
            String passwordHash,
            boolean enabled,
            long credentialVersion) {

        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.enabled = enabled;
        this.credentialVersion = credentialVersion;
    }

    public static ResponsibleUserPrincipal from(
            IdentityUserEntity identityUser) {

        return new ResponsibleUserPrincipal(
                identityUser.userId(),
                identityUser.username(),
                identityUser.passwordHash(),
                identityUser.isEnabled(),
                identityUser.credentialVersion());
    }

    @Override
    public String userId() {
        return userId;
    }

    @Override
    public String username() {
        return username;
    }

    public long credentialVersion() {
        return credentialVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ResponsibleUserPrincipal principal)) {
            return false;
        }

        return userId.equals(principal.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

}
