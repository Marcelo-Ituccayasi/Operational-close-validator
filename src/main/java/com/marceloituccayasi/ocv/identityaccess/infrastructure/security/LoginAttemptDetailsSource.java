package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds trusted details for each form-login authentication attempt.
 */
@Component
public final class LoginAttemptDetailsSource
        implements AuthenticationDetailsSource<
                HttpServletRequest,
                LoginAttemptDetails> {

    private final TrustedClientAddressResolver
            clientAddressResolver;

    public LoginAttemptDetailsSource(
            TrustedClientAddressResolver
                    clientAddressResolver) {

        this.clientAddressResolver =
                clientAddressResolver;
    }

    @Override
    public LoginAttemptDetails buildDetails(
            HttpServletRequest request) {

        String presentedUsername =
                request.getParameter(
                        UsernamePasswordAuthenticationFilter
                                .SPRING_SECURITY_FORM_USERNAME_KEY);

        return new LoginAttemptDetails(
                clientAddressResolver.resolve(request),
                PresentedUsernameNormalizer.normalize(
                        presentedUsername));
    }

}
