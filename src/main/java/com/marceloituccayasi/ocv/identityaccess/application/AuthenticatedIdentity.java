package com.marceloituccayasi.ocv.identityaccess.application;

/**
 * Public identity contract exposed to other application modules.
 */
public interface AuthenticatedIdentity {

    String userId();

    String username();

}
