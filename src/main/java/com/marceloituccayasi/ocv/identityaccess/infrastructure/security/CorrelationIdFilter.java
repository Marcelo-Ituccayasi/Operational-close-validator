package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Assigns one server-owned correlation identifier to every HTTP request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME =
            "X-Correlation-ID";

    public static final String MDC_KEY =
            "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId =
                UUID.randomUUID()
                        .toString();

        String previousCorrelationId =
                MDC.get(
                        MDC_KEY);

        response.setHeader(
                HEADER_NAME,
                correlationId);

        MDC.put(
                MDC_KEY,
                correlationId);

        try {
            filterChain.doFilter(
                    request,
                    response);
        }
        finally {
            restorePreviousCorrelationId(
                    previousCorrelationId);
        }
    }

    private static void restorePreviousCorrelationId(
            String previousCorrelationId) {

        if (previousCorrelationId == null) {
            MDC.remove(
                    MDC_KEY);
            return;
        }

        MDC.put(
                MDC_KEY,
                previousCorrelationId);
    }

}