package com.marceloituccayasi.ocv.operationalclose.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;
import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;

class SecurityContextCurrentActorProviderTest {

    private final SecurityContextCurrentActorProvider provider =
            new SecurityContextCurrentActorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsAuthenticatedIdentityToApplicationPrincipal() {
        AuthenticatedIdentity identity =
                new TestAuthenticatedIdentity(
                        "responsible-user",
                        "responsible");

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        identity,
                        null,
                        List.of());

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        assertThat(provider.currentActor())
                .isEqualTo(new AuthenticatedPrincipal(
                        "responsible-user",
                        "responsible"));
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatThrownBy(provider::currentActor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "No authenticated responsible user is available.");
    }

    @Test
    void rejectsUnsupportedTechnicalPrincipal() {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "unsupported-principal",
                        null,
                        List.of());

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        assertThatThrownBy(provider::currentActor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "No authenticated responsible user is available.");
    }

    private record TestAuthenticatedIdentity(
            String userId,
            String username) implements AuthenticatedIdentity {
    }

}
