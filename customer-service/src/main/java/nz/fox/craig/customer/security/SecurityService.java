package nz.fox.craig.customer.security;

import java.util.UUID;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.exception.UnauthenticatedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public UUID getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthenticatedException();
        }

        return user.id();
    }

    public void verifyCurrentUser(UUID userId) {

        if (!getCurrentUserId().equals(userId)) {
            throw new AccessDeniedException("You may only access your own account.");
        }
    }
}
