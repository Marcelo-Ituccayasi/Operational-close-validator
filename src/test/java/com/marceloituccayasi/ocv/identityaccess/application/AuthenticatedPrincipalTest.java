package com.marceloituccayasi.ocv.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthenticatedPrincipalTest {

    @Test
    void createsFrameworkIndependentPrincipal() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal("responsible-user", "responsible");

        assertThat(principal.userId()).isEqualTo("responsible-user");
        assertThat(principal.username()).isEqualTo("responsible");
    }

    @Test
    void rejectsBlankUserId() {
        assertThatThrownBy(() ->
                new AuthenticatedPrincipal(" ", "responsible"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must not be blank");
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() ->
                new AuthenticatedPrincipal("responsible-user", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
    }

    @Test
    void rejectsNullValues() {
        assertThatThrownBy(() ->
                new AuthenticatedPrincipal(null, "responsible"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must not be blank");

        assertThatThrownBy(() ->
                new AuthenticatedPrincipal("responsible-user", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
    }

}
