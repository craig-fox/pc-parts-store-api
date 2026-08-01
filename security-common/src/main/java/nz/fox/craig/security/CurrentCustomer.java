package nz.fox.craig.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import nz.fox.craig.exception.UnauthenticatedException;

@Component
public class CurrentCustomer {

    public AuthenticatedCustomer get() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
            || !(authentication.getPrincipal() instanceof AuthenticatedCustomer customer)) {
        throw new UnauthenticatedException();
        }

        return customer;
    }

    public UUID customerId() {
        return get().customerId();
    }

    public String email() {
        return get().email();
    }
}
