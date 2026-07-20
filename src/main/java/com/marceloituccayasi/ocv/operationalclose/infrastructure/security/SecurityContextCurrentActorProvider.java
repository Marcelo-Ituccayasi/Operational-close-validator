package com.marceloituccayasi.ocv.operationalclose.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;
import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;
import com.marceloituccayasi.ocv.operationalclose.application.port.CurrentActorProvider;

/**
 * Adapts the authenticated technical principal to the application contract.
 */
@Component
public final class SecurityContextCurrentActorProvider
        implements CurrentActorProvider {

    @Override
    public AuthenticatedPrincipal currentActor() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AuthenticatedIdentity identity)) {

            throw new IllegalStateException(
                    "No authenticated responsible user is available.");
        }

        return new AuthenticatedPrincipal(
                identity.userId(),
                identity.username());
    }

}
