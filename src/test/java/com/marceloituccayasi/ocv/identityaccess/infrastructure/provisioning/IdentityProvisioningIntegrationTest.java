package com.marceloituccayasi.ocv.identityaccess.infrastructure.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import com.marceloituccayasi.ocv.TestcontainersConfiguration;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration.AuthenticationProperties;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.IdentityUserDetailsService;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.ResponsibleUserPrincipal;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.SecurityEventRecorder;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class IdentityProvisioningIntegrationTest {

    @Autowired
    private IdentityUserJpaRepository repository;

    @Autowired
    private ResponsibleUserProvisioner provisioner;

    @Autowired
    private AuthenticationProperties authenticationProperties;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityEventRecorder securityEventRecorder;

    @Autowired
    private UserDetailsService userDetailsService;

    @BeforeEach
    void restoreConfiguredResponsibleUser() {
        repository.deleteAll();
        provisioner.provision();
    }

    @AfterEach
    void leaveConfiguredResponsibleUser() {
        repository.deleteAll();
        provisioner.provision();
    }

    @Test
    void provisionsResponsibleUserWhenTableIsEmpty() {
        repository.deleteAll();

        provisioner.provision();

        IdentityUserEntity identityUser =
                repository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        assertThat(repository.count()).isEqualTo(1L);
        assertThat(identityUser.username())
                .isEqualTo(authenticationProperties.username());
        assertThat(identityUser.usernameNormalized())
                .isEqualTo(authenticationProperties.username());
        assertThat(identityUser.passwordHash())
                .isEqualTo(authenticationProperties.passwordHash());
        assertThat(identityUser.isEnabled()).isTrue();
        assertThat(identityUser.credentialVersion()).isEqualTo(1L);
        assertThat(identityUser.provisionedAt()).isNotNull();
        assertThat(identityUser.updatedAt())
                .isEqualTo(identityUser.provisionedAt());
    }

    @Test
    void repeatedProvisioningDoesNotDuplicateOrRotateCredential() {
        IdentityUserEntity before =
                repository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        Instant originalProvisionedAt = before.provisionedAt();
        Instant originalUpdatedAt = before.updatedAt();

        provisioner.provision();

        IdentityUserEntity after =
                repository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        assertThat(repository.count()).isEqualTo(1L);
        assertThat(after.credentialVersion()).isEqualTo(1L);
        assertThat(after.provisionedAt())
                .isEqualTo(originalProvisionedAt);
        assertThat(after.updatedAt())
                .isEqualTo(originalUpdatedAt);
    }

    @Test
    void credentialRotationIncrementsVersion() {
        IdentityUserEntity before =
                repository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        String rotatedHash =
                passwordEncoder.encode("rotated-test-password");

        AuthenticationProperties rotatedConfiguration =
                new AuthenticationProperties(
                        "RotatedResponsible",
                        rotatedHash);

        ResponsibleUserProvisioner rotatedProvisioner =
                new ResponsibleUserProvisioner(
                        repository,
                        rotatedConfiguration,
                        transactionManager,
                        securityEventRecorder);

        rotatedProvisioner.provision();

        IdentityUserEntity after =
                repository.findById(
                        IdentityUserEntity.RESPONSIBLE_USER_ID)
                        .orElseThrow();

        assertThat(after.username()).isEqualTo("rotatedresponsible");
        assertThat(after.usernameNormalized())
                .isEqualTo("rotatedresponsible");
        assertThat(after.passwordHash()).isEqualTo(rotatedHash);
        assertThat(after.credentialVersion()).isEqualTo(2L);
        assertThat(after.provisionedAt())
                .isEqualTo(before.provisionedAt());
        assertThat(after.updatedAt())
                .isAfterOrEqualTo(before.updatedAt());
    }

    @Test
    void loadsConfiguredUserThroughCustomUserDetailsService() {
        UserDetails loadedUser =
                userDetailsService.loadUserByUsername(
                        "  RESPONSIBLE  ");

        assertThat(userDetailsService)
                .isInstanceOf(IdentityUserDetailsService.class);

        assertThat(loadedUser)
                .isInstanceOf(ResponsibleUserPrincipal.class);

        ResponsibleUserPrincipal principal =
                (ResponsibleUserPrincipal) loadedUser;

        assertThat(principal.userId())
                .isEqualTo(IdentityUserEntity.RESPONSIBLE_USER_ID);
        assertThat(principal.getUsername()).isEqualTo("responsible");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).isEmpty();
        assertThat(principal.credentialVersion()).isEqualTo(1L);
    }

    @Test
    void rejectsUnknownUsernameWithoutDisclosingAccountDetails() {
        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername("unknown-user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciales inválidas.");
    }

}
