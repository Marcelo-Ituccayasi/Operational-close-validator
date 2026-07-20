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
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Servlet security configuration for the single responsible user.
 */
@Configuration(proxyBeanMethods = false)
public class WebSecurityConfiguration {

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
