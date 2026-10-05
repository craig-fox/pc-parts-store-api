package nz.fox.craig.security.service;

import java.util.UUID;

import nz.fox.craig.security.dto.AuthenticatedUser;
import nz.fox.craig.security.exception.UnauthenticatedException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AuthenticatedUser get() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {

            throw new UnauthenticatedException();
        }

        return user;
    }

    public UUID customerId() {
        return get().id();
    }

    public String email() {
        return get().email();
    }
}
