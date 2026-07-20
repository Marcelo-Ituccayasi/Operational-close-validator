package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

/**
 * Enables Spring Security authentication success and failure events.
 */
@Configuration(proxyBeanMethods = false)
public class AuthenticationEventConfiguration {

    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {

        return new DefaultAuthenticationEventPublisher(
                applicationEventPublisher);
    }

}
