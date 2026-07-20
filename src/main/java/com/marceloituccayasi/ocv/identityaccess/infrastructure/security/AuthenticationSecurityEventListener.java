package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;

/**
 * Translates authentication outcomes into persisted OCV events and
 * updates the in-memory login rate limiter.
 */
@Component
public class AuthenticationSecurityEventListener {

    private static final String LOGIN_SUCCEEDED_DETAIL =
            "Authentication succeeded.";

    private static final String LOGIN_FAILED_DETAIL =
            "Authentication failed.";

    private static final String RATE_LIMIT_STARTED_DETAIL =
            "Temporary login rate limit started.";

    private final SecurityEventRecorder recorder;
    private final LoginRateLimiter rateLimiter;

    public AuthenticationSecurityEventListener(
            SecurityEventRecorder recorder,
            LoginRateLimiter rateLimiter) {

        this.recorder = recorder;
        this.rateLimiter = rateLimiter;
    }

    @EventListener
    public void onAuthenticationSuccess(
            AuthenticationSuccessEvent event) {

        Authentication authentication =
                event.getAuthentication();

        LoginAttemptDetails details =
                loginAttemptDetails(authentication);

        if (details != null) {
            rateLimiter.recordSuccess(
                    details.rateLimitKey());
        }

        Object technicalPrincipal =
                authentication.getPrincipal();

        if (technicalPrincipal
                instanceof AuthenticatedIdentity identity) {

            recorder.recordKnownIdentity(
                    SecurityEventType.LOGIN_SUCCEEDED,
                    identity,
                    LOGIN_SUCCEEDED_DETAIL);
        }
    }

    @EventListener
    public void onAuthenticationFailure(
            AbstractAuthenticationFailureEvent event) {

        Authentication authentication =
                event.getAuthentication();

        LoginAttemptDetails details =
                loginAttemptDetails(authentication);

        boolean rateLimitStarted =
                details != null
                        && rateLimiter.recordFailure(
                                details.rateLimitKey());

        Object presentedPrincipal =
                authentication.getPrincipal();

        if (presentedPrincipal
                instanceof AuthenticatedIdentity identity) {

            recorder.recordKnownIdentity(
                    SecurityEventType.LOGIN_FAILED,
                    identity,
                    LOGIN_FAILED_DETAIL);
        }
        else {
            String presentedUsername =
                    details == null
                            ? presentedUsername(
                                    presentedPrincipal)
                            : details.usernameNormalized();

            recorder.recordPresentedUsername(
                    SecurityEventType.LOGIN_FAILED,
                    presentedUsername,
                    LOGIN_FAILED_DETAIL);
        }

        if (rateLimitStarted) {
            recorder.recordPresentedUsername(
                    SecurityEventType.LOGIN_RATE_LIMITED,
                    details.usernameNormalized(),
                    RATE_LIMIT_STARTED_DETAIL);
        }
    }

    private static LoginAttemptDetails loginAttemptDetails(
            Authentication authentication) {

        Object details =
                authentication.getDetails();

        if (details
                instanceof LoginAttemptDetails
                        loginAttemptDetails) {

            return loginAttemptDetails;
        }

        return null;
    }

    private static String presentedUsername(
            Object principal) {

        if (principal == null) {
            return null;
        }

        return PresentedUsernameNormalizer.normalize(
                principal.toString());
    }

}
