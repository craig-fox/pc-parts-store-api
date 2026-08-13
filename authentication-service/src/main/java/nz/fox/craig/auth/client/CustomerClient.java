package nz.fox.craig.auth.client;

import nz.fox.craig.auth.dto.AuthenticatedCustomer;

public interface CustomerClient {

    AuthenticatedCustomer findByEmail(String email);
}
