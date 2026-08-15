package nz.fox.craig.customer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;
import nz.fox.craig.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID OTHER_CUSTOMER_ID = UUID.randomUUID();

    private SecurityService securityService;

    @BeforeEach
    void setUp() {

        securityService = new SecurityService();

        AuthenticatedUser user =
                new AuthenticatedUser(CUSTOMER_ID, "jane@example.com", Set.of(Role.ROLE_CUSTOMER));

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetCurrentUserId {

        @Test
        void returnsCurrentUserId() {
            UUID customerId = securityService.getCurrentUserId();

            assertThat(customerId).isEqualTo(CUSTOMER_ID);
        }

        @Test
        void throwsWhenUserIsUnauthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> securityService.getCurrentUserId())
                    .isInstanceOf(UnauthenticatedException.class);
        }
    }

    @Nested
    class VerifyCurrentUser {

        @Test
        void allowsCurrentUser() {
            assertThatCode(() -> securityService.verifyCurrentUser(CUSTOMER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejectsDifferentCustomer() {
            assertThatThrownBy(() -> securityService.verifyCurrentUser(OTHER_CUSTOMER_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You may only access your own account.");
        }
    }
}
