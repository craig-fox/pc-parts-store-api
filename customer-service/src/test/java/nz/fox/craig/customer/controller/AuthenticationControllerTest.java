package nz.fox.craig.customer.controller;



import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.fox.craig.customer.config.SecurityConfig;
import nz.fox.craig.customer.dto.LoginRequest;
import nz.fox.craig.customer.dto.LoginResponse;
import nz.fox.craig.customer.exception.CustomerExceptionHandler;
import nz.fox.craig.customer.exception.InvalidCredentialsException;
import nz.fox.craig.customer.security.AuthenticationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;

@WebMvcTest(AuthenticationController.class)
@Import({
    SecurityConfig.class,
    CustomerExceptionHandler.class
})
class AuthenticationControllerTest {

    private final String USER_EMAIL = "jane@example.com";
    private final String LOGIN_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void shouldLoginSuccessfully() throws Exception {

        final LoginRequest request = new LoginRequest(USER_EMAIL, LOGIN_PASSWORD);

        final LoginResponse response = new LoginResponse("jwt-token", UUID.randomUUID(), "Jane", "Jo");

        when(authenticationService.login(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {

        final LoginRequest request = new LoginRequest("", LOGIN_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingPassword() throws Exception {

        final LoginRequest request = new LoginRequest(USER_EMAIL, "");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {

        final LoginRequest request = new LoginRequest(USER_EMAIL, "wrong");

        when(authenticationService.login(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
