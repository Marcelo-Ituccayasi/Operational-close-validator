package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;

/**
 * Loads the configured responsible user from PostgreSQL.
 */
@Service
public final class IdentityUserDetailsService implements UserDetailsService {

    private static final String INVALID_CREDENTIALS =
            "Credenciales inválidas.";

    private final IdentityUserJpaRepository repository;

    public IdentityUserDetailsService(
            IdentityUserJpaRepository repository) {

        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String presentedUsername) {
        String normalizedUsername =
                normalizeUsername(presentedUsername);

        return repository
                .findByUsernameNormalized(normalizedUsername)
                .map(ResponsibleUserPrincipal::from)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                INVALID_CREDENTIALS));
    }

    private static String normalizeUsername(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

}
