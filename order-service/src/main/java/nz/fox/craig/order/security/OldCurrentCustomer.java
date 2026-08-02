package nz.fox.craig.order.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

//@Component
public class OldCurrentCustomer {

    public UUID getCustomerId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return (UUID) authentication.getPrincipal();
    }
}
