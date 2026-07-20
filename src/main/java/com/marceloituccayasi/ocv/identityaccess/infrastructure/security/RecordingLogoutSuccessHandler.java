package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Records an explicit logout and redirects to the public login page.
 */
@Component
public final class RecordingLogoutSuccessHandler
        implements LogoutSuccessHandler {

    private static final String LOGOUT_DETAIL =
            "Logout completed.";

    private final SecurityEventRecorder recorder;
    private final SimpleUrlLogoutSuccessHandler redirectHandler;

    public RecordingLogoutSuccessHandler(
            SecurityEventRecorder recorder) {

        this.recorder = recorder;
        this.redirectHandler =
                new SimpleUrlLogoutSuccessHandler();

        this.redirectHandler.setDefaultTargetUrl(
                "/login?logout");
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (authentication != null
                && authentication.getPrincipal()
                instanceof AuthenticatedIdentity identity) {

            recorder.recordKnownIdentity(
                    SecurityEventType.LOGOUT,
                    identity,
                    LOGOUT_DETAIL);
        }

        redirectHandler.onLogoutSuccess(
                request,
                response,
                authentication);
    }

}
