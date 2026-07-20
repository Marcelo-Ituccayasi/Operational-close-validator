package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration.TrustedProxyProperties;

class TrustedClientAddressResolverTest {

    @Test
    void usesDirectPeerWhenNoTrustedProxyIsConfigured() {
        TrustedClientAddressResolver resolver =
                resolverWithCidrs("");

        MockHttpServletRequest request =
                requestFrom("203.0.113.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.20");

        assertThat(resolver.resolve(request))
                .isEqualTo("203.0.113.10");
    }

    @Test
    void ignoresForwardedHeaderFromUntrustedDirectPeer() {
        TrustedClientAddressResolver resolver =
                resolverWithCidrs("10.0.0.0/8");

        MockHttpServletRequest request =
                requestFrom("203.0.113.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.20");

        assertThat(resolver.resolve(request))
                .isEqualTo("203.0.113.10");
    }

    @Test
    void resolvesClientThroughTrustedProxyChain() {
        TrustedClientAddressResolver resolver =
                resolverWithCidrs("10.0.0.0/8");

        MockHttpServletRequest request =
                requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.20, 10.0.0.9");

        assertThat(resolver.resolve(request))
                .isEqualTo("198.51.100.20");
    }

    @Test
    void supportsStandardForwardedHeader() {
        TrustedClientAddressResolver resolver =
                resolverWithCidrs("10.0.0.0/8");

        MockHttpServletRequest request =
                requestFrom("10.0.0.10");

        request.addHeader(
                "Forwarded",
                "for=198.51.100.21;proto=https, "
                        + "for=10.0.0.9");

        assertThat(resolver.resolve(request))
                .isEqualTo("198.51.100.21");
    }

    @Test
    void fallsBackToDirectPeerForMalformedHeader() {
        TrustedClientAddressResolver resolver =
                resolverWithCidrs("10.0.0.0/8");

        MockHttpServletRequest request =
                requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "attacker.example");

        assertThat(resolver.resolve(request))
                .isEqualTo("10.0.0.10");
    }

    @Test
    void rejectsInvalidConfiguredCidr() {
        assertThatThrownBy(() ->
                resolverWithCidrs("10.0.0.0/99"))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage(
                        "Invalid trusted proxy CIDR: "
                                + "10.0.0.0/99");
    }

    private static TrustedClientAddressResolver
            resolverWithCidrs(String cidrs) {

        TrustedProxyProperties properties =
                new TrustedProxyProperties();

        properties.setCidrs(cidrs);

        return new TrustedClientAddressResolver(
                properties);
    }

    private static MockHttpServletRequest requestFrom(
            String remoteAddress) {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRemoteAddr(remoteAddress);
        return request;
    }

}
