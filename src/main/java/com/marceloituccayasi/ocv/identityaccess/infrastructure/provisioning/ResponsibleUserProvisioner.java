package com.marceloituccayasi.ocv.identityaccess.infrastructure.provisioning;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration.AuthenticationProperties;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.entity.IdentityUserEntity;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.persistence.repository.IdentityUserJpaRepository;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.SecurityEventRecorder;
import com.marceloituccayasi.ocv.identityaccess.infrastructure.security.SecurityEventType;

/**
 * Creates or synchronizes the single externally configured responsible user.
 */
@Component
public final class ResponsibleUserProvisioner implements ApplicationRunner {

    private static final String USER_PROVISIONED_DETAIL =
            "Responsible user provisioned.";

    private static final String CREDENTIAL_ROTATED_DETAIL =
            "Responsible user credential rotated.";

    private final IdentityUserJpaRepository repository;
    private final AuthenticationProperties authenticationProperties;
    private final SecurityEventRecorder securityEventRecorder;
    private final TransactionTemplate transactionTemplate;

    public ResponsibleUserProvisioner(
            IdentityUserJpaRepository repository,
            AuthenticationProperties authenticationProperties,
            PlatformTransactionManager transactionManager,
            SecurityEventRecorder securityEventRecorder) {

        this.repository = repository;
        this.authenticationProperties = authenticationProperties;
        this.securityEventRecorder = securityEventRecorder;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

        this.transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void run(ApplicationArguments arguments) {
        provision();
    }

    public void provision() {
        transactionTemplate.executeWithoutResult(status ->
                synchronizeResponsibleUser());
    }

    private void synchronizeResponsibleUser() {
        List<IdentityUserEntity> existingUsers =
                repository.findAll();

        if (existingUsers.size() > 1) {
            throw new IllegalStateException(
                    "Identity configuration is incompatible: "
                            + "more than one identity user exists.");
        }

        Instant now = Instant.now();
        String configuredUsername =
                authenticationProperties.username();

        if (existingUsers.isEmpty()) {
            IdentityUserEntity provisionedUser =
                    IdentityUserEntity.provision(
                            configuredUsername,
                            configuredUsername,
                            authenticationProperties.passwordHash(),
                            now);

            repository.saveAndFlush(provisionedUser);

            securityEventRecorder.recordKnownIdentity(
                    SecurityEventType.USER_PROVISIONED,
                    provisionedUser.userId(),
                    provisionedUser.usernameNormalized(),
                    USER_PROVISIONED_DETAIL);

            return;
        }

        IdentityUserEntity existingUser =
                existingUsers.getFirst();

        if (!IdentityUserEntity.RESPONSIBLE_USER_ID.equals(
                existingUser.userId())) {

            throw new IllegalStateException(
                    "Identity configuration is incompatible: "
                            + "the configured responsible user is missing.");
        }

        boolean passwordChanged =
                !existingUser.passwordHash().equals(
                        authenticationProperties.passwordHash());

        boolean changed = existingUser.synchronize(
                configuredUsername,
                configuredUsername,
                authenticationProperties.passwordHash(),
                now);

        if (!changed) {
            return;
        }

        repository.saveAndFlush(existingUser);

        if (passwordChanged) {
            securityEventRecorder.recordKnownIdentity(
                    SecurityEventType.CREDENTIAL_ROTATED,
                    existingUser.userId(),
                    existingUser.usernameNormalized(),
                    CREDENTIAL_ROTATED_DETAIL);
        }
    }

}
