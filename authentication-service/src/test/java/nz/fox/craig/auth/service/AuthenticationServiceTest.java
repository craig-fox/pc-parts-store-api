package nz.fox.craig.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import nz.fox.craig.auth.client.CustomerClient;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;
import nz.fox.craig.auth.dto.LoginRequest;
import nz.fox.craig.auth.dto.LoginResponse;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String LOGIN_PASSWORD = "Password123!";

    @Mock private CustomerClient customerClient;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private TokenService tokenService;

    @InjectMocks private AuthenticationService authenticationService;

    @Test
    void shouldLoginSuccessfully() {

        AuthenticatedCustomer customer = authenticatedCustomer(true);

        when(customerClient.findByEmail(customer.email())).thenReturn(customer);

        when(passwordEncoder.matches(LOGIN_PASSWORD, customer.password())).thenReturn(true);

        when(tokenService.generateToken(any(AuthenticatedUser.class))).thenReturn("mock-token");

        LoginResponse response =
                authenticationService.login(new LoginRequest(customer.email(), LOGIN_PASSWORD));

        assertEquals("mock-token", response.token());
        assertEquals(customer.id(), response.customerId());
        assertEquals(customer.firstName(), response.firstName());
        assertEquals(customer.preferredName(), response.preferredName());

        verify(customerClient).findByEmail(customer.email());
        verify(passwordEncoder).matches(LOGIN_PASSWORD, customer.password());
        verify(tokenService).generateToken(any(AuthenticatedUser.class));
    }

    @Test
    void passwordHashMatches() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = "$2a$10$nalmCvkxSWfwt/elgq0Yo.wcbCxCvxdetuJ6qkYMjSFj/Puk1sWcG";

        assertTrue(encoder.matches("myPassword123", hash));
    }

    @Test
    void shouldRejectUnknownEmail() {}

    @Test
    void shouldRejectWrongPassword() {}

    @Test
    void shouldRejectLoginForInactiveCustomer() {}

    private AuthenticatedCustomer authenticatedCustomer(boolean active) {

        return new AuthenticatedCustomer(
                UUID.randomUUID(), "jane@example.com", "encoded-password", active, "Jane", "Jo");
    }
}
