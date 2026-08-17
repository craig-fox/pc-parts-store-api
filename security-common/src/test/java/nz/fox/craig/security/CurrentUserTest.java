package nz.fox.craig.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthenticatedUser() {
        var user = SampleAuthenticatedUsers.authenticatedCustomerUser();
        authenticate(user);

        assertThat(currentUser.get()).isEqualTo(user);
    }

    @Test
    void shouldThrowWhenAuthenticationIsMissing() {
        assertThatThrownBy(currentUser::get)
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldThrowWhenPrincipalIsNotAuthenticatedUser() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn("unexpected-principal");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(currentUser::get)
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldReturnCustomerId() {
        UUID customerId = UUID.randomUUID();
        authenticate(SampleAuthenticatedUsers.authenticatedCustomerUser(customerId));

        assertThat(currentUser.customerId())
                .isEqualTo(customerId);
    }

    @Test
    void shouldReturnEmail() {
        authenticate(SampleAuthenticatedUsers.authenticatedCustomerUser());

        assertThat(currentUser.email())
                .isEqualTo("test@example.com");
    }

    private void authenticate(AuthenticatedUser user) {
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
