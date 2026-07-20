package com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;

/**
 * Internal persistence repository owned by identity and access.
 */
public interface IdentityUserJpaRepository
        extends JpaRepository<IdentityUserEntity, String> {

    Optional<IdentityUserEntity> findByUsernameNormalized(
            String usernameNormalized);

}
