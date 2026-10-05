package nz.fox.craig.security.service;

import java.util.Set;
import java.util.UUID;

import nz.fox.craig.security.dto.AuthenticatedUser;
import nz.fox.craig.security.dto.Role;

public class SampleAuthenticatedUsers {

    private SampleAuthenticatedUsers() {
        super();
    }

    public static AuthenticatedUser authenticatedCustomerUser() {
        return authenticatedCustomerUser(UUID.randomUUID());
    }

    public static AuthenticatedUser authenticatedCustomerUser(UUID userToken) {
        Set<Role> roles = Set.of(Role.ROLE_CUSTOMER);

        return new AuthenticatedUser(userToken, "test@example.com", roles);
    }
}
