package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.io.IOException;

import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Distinguishes an actually invalid session identifier from a request
 * rejected because its CSRF token is missing.
 */
@Component
public final class RecordingInvalidSessionStrategy
        implements InvalidSessionStrategy {

    private static final String SESSION_EXPIRED_DETAIL =
            "Expired session identifier presented.";

    private final SecurityEventRecorder recorder;
    private final InvalidSessionStrategy redirectStrategy;

    public RecordingInvalidSessionStrategy(
            SecurityEventRecorder recorder) {

        this.recorder = recorder;
        this.redirectStrategy =
                new SimpleRedirectInvalidSessionStrategy(
                        "/login?expired");
    }

    @Override
    public void onInvalidSessionDetected(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {

        if (!hasInvalidRequestedSession(request)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        recorder.recordPresentedUsername(
                SecurityEventType.SESSION_EXPIRED,
                null,
                SESSION_EXPIRED_DETAIL);

        redirectStrategy.onInvalidSessionDetected(
                request,
                response);
    }

    private static boolean hasInvalidRequestedSession(
            HttpServletRequest request) {

        return request.getRequestedSessionId() != null
                && !request.isRequestedSessionIdValid();
    }

}
