package nz.fox.craig.auth.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import nz.fox.craig.auth.client.CustomerClient;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;
import nz.fox.craig.auth.dto.LoginRequest;
import nz.fox.craig.auth.exception.InvalidCredentialsException;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;
import nz.fox.craig.security.TokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    private static final String LOGIN_PASSWORD = "Password123!";
    private static final String JANE_EMAIL = "jane@example.com";
    private static final String UNKNOWN_EMAIL = "unknown@example.com";

    @MockitoBean private CustomerClient customerClient;

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private PasswordEncoder encoder;

    @MockitoBean private TokenService tokenService;

    @Test
    void shouldLoginSuccessfully() throws Exception {

        String loginPassword = LOGIN_PASSWORD;

        AuthenticatedCustomer jane = authenticatedCustomer(JANE_EMAIL, true, "Jo");

        String mockToken = "mock-jwt-token";

        when(customerClient.findByEmail("jane@example.com")).thenReturn(jane);

        when(tokenService.generateToken(any(AuthenticatedUser.class))).thenReturn(mockToken);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(JANE_EMAIL, loginPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(mockToken))
                .andExpect(jsonPath("$.customerId").value(jane.id().toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.preferredName").value("Jo"));

        verify(customerClient).findByEmail(JANE_EMAIL);

        ArgumentCaptor<AuthenticatedUser> userCaptor =
                ArgumentCaptor.forClass(AuthenticatedUser.class);

        verify(tokenService).generateToken(userCaptor.capture());

        AuthenticatedUser authenticatedUser = userCaptor.getValue();

        assertEquals(jane.id(), authenticatedUser.id());
        assertEquals(JANE_EMAIL, authenticatedUser.email());
        assertEquals(Set.of(Role.ROLE_CUSTOMER), authenticatedUser.roles());
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {

        when(customerClient.findByEmail("unknown@example.com"))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(UNKNOWN_EMAIL, LOGIN_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(customerClient).findByEmail(UNKNOWN_EMAIL);
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(null, LOGIN_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email is required"));

        verifyNoInteractions(customerClient);
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldRejectMissingPassword() throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(JANE_EMAIL, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password is required"));
        verifyNoInteractions(customerClient);
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldRejectInvalidEmailFormat() throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest("wrongformat", LOGIN_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email must be valid"));

        verifyNoInteractions(customerClient);
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {

        AuthenticatedCustomer jane = authenticatedCustomer(JANE_EMAIL, true);

        when(customerClient.findByEmail(JANE_EMAIL)).thenReturn(jane);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(JANE_EMAIL, "WrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        ;

        verify(customerClient).findByEmail("jane@example.com");
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void shouldRejectLoginForInactiveCustomer() throws Exception {

        AuthenticatedCustomer jane = authenticatedCustomer(JANE_EMAIL, false);

        when(customerClient.findByEmail(JANE_EMAIL)).thenReturn(jane);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(jane.email(), LOGIN_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Customer account is inactive"));

        verify(customerClient).findByEmail(JANE_EMAIL);
        verify(tokenService, never()).generateToken(any());
    }

    private AuthenticatedCustomer authenticatedCustomer(
            String email, boolean active, String... preferredName) {
        String passwordHash = encoder.encode(LOGIN_PASSWORD);
        UUID customerId = UUID.randomUUID();
        String preferred = (preferredName.length > 0) ? preferredName[0] : null;

        return new AuthenticatedCustomer(
                customerId, email, passwordHash, active, "Jane", preferred);
    }
}
