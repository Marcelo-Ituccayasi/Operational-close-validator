package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional comma-separated trusted proxy CIDR configuration.
 *
 * <p>The corresponding environment variable is
 * {@code OCV_TRUSTED_PROXY_CIDRS}.
 */
@ConfigurationProperties(prefix = "ocv.trusted.proxy")
public final class TrustedProxyProperties {

    private String cidrs = "";

    public String getCidrs() {
        return cidrs;
    }

    public void setCidrs(String cidrs) {
        this.cidrs = cidrs == null ? "" : cidrs.trim();
    }

}
