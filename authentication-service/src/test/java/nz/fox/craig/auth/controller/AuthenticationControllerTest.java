package nz.fox.craig.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import nz.fox.craig.auth.dto.LoginRequest;
import nz.fox.craig.auth.dto.LoginResponse;
import nz.fox.craig.auth.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthenticationService authenticationService;

    @Test
    @DisplayName("Should login successfully")
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
    @DisplayName("Should return 400 when email is missing")
    void shouldReturn400WhenEmailMissing() throws Exception {

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
    }

    @Test
    @DisplayName("Should return 400 when password is missing")
    void shouldReturn400WhenPasswordMissing() throws Exception {

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
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {

        LoginRequest request = new LoginRequest("not-an-email", "password");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
