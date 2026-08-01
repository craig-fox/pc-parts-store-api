package nz.fox.craig.customer.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public UUID getCurrentCustomerId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomerUserDetails user =
                (CustomerUserDetails) authentication.getPrincipal();

        return user.getCustomerId();
    }

    public void verifyCurrentCustomer(UUID customerId) {

        if (!getCurrentCustomerId().equals(customerId)) {
            throw new AccessDeniedException(
                "You may only access your own account.");
        }
    }
}
