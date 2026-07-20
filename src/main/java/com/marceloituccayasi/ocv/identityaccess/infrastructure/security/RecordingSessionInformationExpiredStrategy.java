package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.io.IOException;

import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedIdentity;

import jakarta.servlet.ServletException;

/**
 * Records expiration caused by a newer login replacing the previous session.
 */
@Component
public final class RecordingSessionInformationExpiredStrategy
        implements SessionInformationExpiredStrategy {

    private static final String SESSION_REPLACED_DETAIL =
            "Previous session replaced by a newer authentication.";

    private final SecurityEventRecorder recorder;
    private final SessionInformationExpiredStrategy redirectStrategy;

    public RecordingSessionInformationExpiredStrategy(
            SecurityEventRecorder recorder) {

        this.recorder = recorder;
        this.redirectStrategy =
                new SimpleRedirectSessionInformationExpiredStrategy(
                        "/login?replaced");
    }

    @Override
    public void onExpiredSessionDetected(
            SessionInformationExpiredEvent event)
            throws IOException, ServletException {

        Object principal =
                event.getSessionInformation().getPrincipal();

        if (principal instanceof AuthenticatedIdentity identity) {
            recorder.recordKnownIdentity(
                    SecurityEventType.SESSION_REPLACED,
                    identity,
                    SESSION_REPLACED_DETAIL);
        }

        redirectStrategy.onExpiredSessionDetected(event);
    }

}
