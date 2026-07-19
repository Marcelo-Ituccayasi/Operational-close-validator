package com.marceloituccayasi.ocv.operationalclose.application.port;

import com.marceloituccayasi.ocv.identityaccess.application.AuthenticatedPrincipal;

/**
 * Provides the authenticated actor required by operational close use cases.
 */
@FunctionalInterface
public interface CurrentActorProvider {

    AuthenticatedPrincipal currentActor();

}
