package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stops authentication before password verification when a login key
 * is temporarily blocked.
 */
public final class LoginRateLimitFilter
        extends OncePerRequestFilter {

    private static final String RATE_LIMITED_DETAIL =
            "Login attempt rejected by temporary rate limit.";

    private final RequestMatcher loginRequestMatcher =
            pathPattern(
                    HttpMethod.POST,
                    "/login");

    private final LoginRateLimiter rateLimiter;
    private final TrustedClientAddressResolver
            clientAddressResolver;
    private final SecurityEventRecorder
            securityEventRecorder;

    private final SimpleUrlAuthenticationFailureHandler
            failureHandler =
            new SimpleUrlAuthenticationFailureHandler(
                    "/login?error");

    public LoginRateLimitFilter(
            LoginRateLimiter rateLimiter,
            TrustedClientAddressResolver
                    clientAddressResolver,
            SecurityEventRecorder securityEventRecorder) {

        this.rateLimiter = rateLimiter;
        this.clientAddressResolver =
                clientAddressResolver;
        this.securityEventRecorder =
                securityEventRecorder;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        return !loginRequestMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String usernameNormalized =
                PresentedUsernameNormalizer.normalize(
                        request.getParameter(
                                UsernamePasswordAuthenticationFilter
                                        .SPRING_SECURITY_FORM_USERNAME_KEY));

        LoginRateLimitKey key =
                new LoginRateLimitKey(
                        clientAddressResolver.resolve(request),
                        usernameNormalized);

        if (!rateLimiter.isBlocked(key)) {
            filterChain.doFilter(
                    request,
                    response);

            return;
        }

        securityEventRecorder.recordPresentedUsername(
                SecurityEventType.LOGIN_RATE_LIMITED,
                usernameNormalized,
                RATE_LIMITED_DETAIL);

        failureHandler.onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException(
                        "Invalid credentials."));
    }

}
