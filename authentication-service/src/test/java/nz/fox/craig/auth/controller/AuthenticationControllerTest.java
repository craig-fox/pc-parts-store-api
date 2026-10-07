package nz.fox.craig.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import nz.fox.craig.api.DownstreamServiceUnavailableException;
import nz.fox.craig.auth.dto.LoginRequest;
import nz.fox.craig.auth.dto.LoginResponse;
import nz.fox.craig.auth.exception.CustomerInactiveException;
import nz.fox.craig.auth.exception.InvalidCredentialsException;
import nz.fox.craig.auth.service.AuthenticationService;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper objectMapper;

    @MockitoBean private AuthenticationService authenticationService;

    @Nested
    class Login {

        @Test
        void shouldLoginSuccessfully() throws Exception {

            LoginRequest request = new LoginRequest("craig@example.com", "password");

            UUID customerId = UUID.randomUUID();

            LoginResponse response = new LoginResponse("jwt-token", customerId, "Craig", "Craig");

            given(authenticationService.login(any(LoginRequest.class))).willReturn(response);

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.firstName").value("Craig"))
                    .andExpect(jsonPath("$.preferredName").value("Craig"));
        }

        @Test
        void shouldReturn400WhenEmailIsMissing() throws Exception {
            String json =
                    """
                    {
                      "password": "password"
                    }
                    """;
        
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest());
        
            then(authenticationService).shouldHaveNoInteractions();
        }

        @Test
        void shouldReturn400WhenPasswordIsMissing() throws Exception {

            String json =
                    """
                    {
                      "email": "craig@example.com"
                    }
                    """;

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                    .andExpect(status().isBadRequest());
            then(authenticationService).shouldHaveNoInteractions();
        }

        @Test
        void shouldReturn400WhenEmailIsInvalid() throws Exception {

            LoginRequest request = new LoginRequest("not-an-email", "password");

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            then(authenticationService).shouldHaveNoInteractions();
        }

        @Test
        void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
            LoginRequest request =
                    new LoginRequest("craig@example.com", "wrong-password");

            given(authenticationService.login(any(LoginRequest.class)))
                    .willThrow(new InvalidCredentialsException());

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401WhenCustomerIsInactive() throws Exception {
            LoginRequest request =
                    new LoginRequest("craig@example.com", "password");

            given(authenticationService.login(any(LoginRequest.class)))
                    .willThrow(new CustomerInactiveException());

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn502WhenDownstreamServiceUnavailable() throws Exception {
            LoginRequest request =
                    new LoginRequest("craig@example.com", "password");

            given(authenticationService.login(any(LoginRequest.class)))
                    .willThrow(new DownstreamServiceUnavailableException("customer-service", new Throwable()));

            mockMvc.perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }
}
