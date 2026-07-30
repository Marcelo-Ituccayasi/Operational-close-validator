package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Servlet security configuration for the single responsible user.
 */
@Configuration(proxyBeanMethods = false)
public class WebSecurityConfiguration {

    private static final long HSTS_MAX_AGE_SECONDS =
            31_536_000L;

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; "
                    + "object-src 'none'; "
                    + "base-uri 'self'; "
                    + "frame-ancestors 'none'; "
                    + "form-action 'self'; "
                    + "script-src 'self'; "
                    + "style-src 'self'; "
                    + "img-src 'self';";

    private static final String PERMISSIONS_POLICY =
            "geolocation=(), microphone=(), camera=()";

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            RecordingLogoutSuccessHandler logoutSuccessHandler,
            RecordingSessionInformationExpiredStrategy
                    replacedSessionStrategy,
            RecordingInvalidSessionStrategy invalidSessionStrategy,
            LoginAttemptDetailsSource loginAttemptDetailsSource,
            LoginRateLimiter loginRateLimiter,
            TrustedClientAddressResolver clientAddressResolver,
            SecurityEventRecorder securityEventRecorder)
            throws Exception {

        LoginRateLimitFilter loginRateLimitFilter =
                new LoginRateLimitFilter(
                        loginRateLimiter,
                        clientAddressResolver,
                        securityEventRecorder);

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/css/**",
                                "/images/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .authenticationDetailsSource(
                                loginAttemptDetailsSource)
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(
                                pathPattern(
                                        HttpMethod.POST,
                                        "/logout"))
                        .logoutSuccessHandler(
                                logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .csrf(Customizer.withDefaults())
                .headers(headers -> headers
                        .cacheControl(
                                Customizer.withDefaults())
                        .contentTypeOptions(
                                Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(false)
                                .maxAgeInSeconds(
                                        HSTS_MAX_AGE_SECONDS))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        CONTENT_SECURITY_POLICY))
                        .frameOptions(frameOptions ->
                                frameOptions.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(
                                        ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy(
                                        PERMISSIONS_POLICY)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionStrategy(
                                invalidSessionStrategy)
                        .sessionFixation(fixation ->
                                fixation.changeSessionId())
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredSessionStrategy(
                                        replacedSessionStrategy)
                                .sessionRegistry(sessionRegistry)))
                .addFilterAfter(
                        loginRateLimitFilter,
                        org.springframework.security.web.csrf.CsrfFilter.class);

        return http.build();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

}