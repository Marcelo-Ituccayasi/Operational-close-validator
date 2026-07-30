package com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers validated external configuration for identity and access.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        AuthenticationProperties.class,
        LoginRateLimitProperties.class,
        TrustedProxyProperties.class
})
public class IdentityAccessPropertiesConfiguration {

}
