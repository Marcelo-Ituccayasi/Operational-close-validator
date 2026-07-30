package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class CorrelationIdFilterTest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-"
                    + "4[0-9a-f]{3}-"
                    + "[89ab][0-9a-f]{3}-"
                    + "[0-9a-f]{12}";

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter();

    @AfterEach
    void clearMappedDiagnosticContext() {
        MDC.clear();
    }

    @Test
    void generatesServerOwnedCorrelationIdForRequest()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "client-controlled");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> observedCorrelationId =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        observedCorrelationId.set(
                                MDC.get(
                                        CorrelationIdFilter.MDC_KEY)));

        String responseCorrelationId =
                response.getHeader(
                        CorrelationIdFilter.HEADER_NAME);

        assertThat(
                responseCorrelationId)
                .matches(
                        UUID_PATTERN)
                .isNotEqualTo(
                        "client-controlled");

        assertThat(
                observedCorrelationId)
                .hasValue(
                        responseCorrelationId);

        assertThat(
                MDC.get(
                        CorrelationIdFilter.MDC_KEY))
                .isNull();
    }

    @Test
    void restoresPreviousCorrelationIdAfterRequest()
            throws Exception {

        MDC.put(
                CorrelationIdFilter.MDC_KEY,
                "outer-correlation");

        AtomicReference<String> requestCorrelationId =
                new AtomicReference<>();

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) ->
                        requestCorrelationId.set(
                                MDC.get(
                                        CorrelationIdFilter.MDC_KEY)));

        assertThat(
                requestCorrelationId.get())
                .matches(
                        UUID_PATTERN)
                .isNotEqualTo(
                        "outer-correlation");

        assertThat(
                MDC.get(
                        CorrelationIdFilter.MDC_KEY))
                .isEqualTo(
                        "outer-correlation");
    }

    @Test
    void restoresMappedDiagnosticContextWhenRequestFails() {

        MDC.put(
                CorrelationIdFilter.MDC_KEY,
                "outer-correlation");

        assertThatThrownBy(() ->
                filter.doFilter(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        (request, response) -> {
                            throw new ServletException(
                                    "test failure");
                        }))
                .isInstanceOf(
                        ServletException.class)
                .hasMessage(
                        "test failure");

        assertThat(
                MDC.get(
                        CorrelationIdFilter.MDC_KEY))
                .isEqualTo(
                        "outer-correlation");
    }

}