package nz.fox.craig.customer.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import nz.fox.craig.customer.common.AbstractPostgresTest;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.dto.LoginRequest;
import nz.fox.craig.customer.dto.LoginResponse;
import nz.fox.craig.customer.repository.CustomerRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest extends AbstractPostgresTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;


    @Test
    void shouldRegisterLoginAndRetrieveOwnCustomer() throws Exception {

        // Register

        CustomerRequest registerRequest =
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!");

        MvcResult registrationResult =
                mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                        .andExpect(status().isCreated())
                        .andReturn();

        CustomerResponse customer =
                objectMapper.readValue(
                        registrationResult.getResponse().getContentAsString(),
                        CustomerResponse.class);

        // Login

        LoginRequest loginRequest =
                new LoginRequest(
                        "jane@example.com",
                        "Password123!");

        MvcResult loginResult =
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isOk())
                        .andReturn();

        LoginResponse login =
                objectMapper.readValue(
                        loginResult.getResponse().getContentAsString(),
                        LoginResponse.class);

        // Retrieve own customer

        mockMvc.perform(get("/api/customers/" + customer.id())
                        .header(
                                "Authorization",
                                "Bearer " + login.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(customer.id().toString()))
                .andExpect(jsonPath("$.firstName")
                        .value("Jane"))
                .andExpect(jsonPath("$.lastName")
                        .value("Doe"))
                .andExpect(jsonPath("$.displayName")
                        .value("Jo"))
                .andExpect(jsonPath("$.email")
                        .value("jane@example.com"));
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {

        CustomerRequest registerRequest =
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!");

        MvcResult registrationResult =
                mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                        .andExpect(status().isCreated())
                        .andReturn();

        CustomerResponse customer =
                objectMapper.readValue(
                        registrationResult.getResponse().getContentAsString(),
                        CustomerResponse.class);

        mockMvc.perform(get("/api/customers/" + customer.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccessToAnotherCustomer() throws Exception {

        // Register Jane

        CustomerRequest janeRequest =
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!");

        CustomerResponse jane =
                objectMapper.readValue(
                        mockMvc.perform(post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(janeRequest)))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        CustomerResponse.class);

        // Register John

        CustomerRequest johnRequest =
                new CustomerRequest(
                        "John",
                        "Smith",
                        null,
                        "john@example.com",
                        "456 Queen Street",
                        "Password123!");

        CustomerResponse john =
                objectMapper.readValue(
                        mockMvc.perform(post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(johnRequest)))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        CustomerResponse.class);

        // Login as Jane

        LoginRequest loginRequest =
                new LoginRequest(
                        "jane@example.com",
                        "Password123!");

        LoginResponse login =
                objectMapper.readValue(
                        mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        LoginResponse.class);

        // Attempt to retrieve John's profile

        mockMvc.perform(get("/api/customers/" + john.id())
                        .header(
                                "Authorization",
                                "Bearer " + login.token()))
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldRejectInvalidJwt() throws Exception {

        mockMvc.perform(get("/api/customers/" + UUID.randomUUID())
                .header("Authorization", "Bearer this-is-not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        register(
            new CustomerRequest(
                    "Jane",
                    "Doe",
                    "Jo",
                    "jane@example.com",
                    "123 Main Street",
                    "Password123!"));
        mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(
                new LoginRequest(
                        "jane@example.com",
                        "WrongPassword"))))
        .andExpect(status().isUnauthorized());            

    }

    @Test
    void shouldRejectUpdatingAnotherCustomer() throws Exception {

        CustomerResponse jane = register(
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!"));

        CustomerResponse john = register(
                new CustomerRequest(
                        "John",
                        "Smith",
                        null,
                        "john@example.com",
                        "456 Queen Street",
                        "Password123!"));

        String token = login(
                "jane@example.com",
                "Password123!");

        CustomerRequest updateRequest =
                new CustomerRequest(
                        "John Updated",
                        "Smith",
                        "Johnny",
                        "john@example.com",
                        "789 New Street",
                        "Password123!");

        mockMvc.perform(put("/api/customers/" + john.id())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    private CustomerResponse register(CustomerRequest request) throws Exception {

        MvcResult result =
                mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andReturn();
    
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CustomerResponse.class);
    }

    private String login(String email, String password) throws Exception {

        LoginRequest request = new LoginRequest(email, password);
    
        MvcResult result =
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn();
    
        LoginResponse response =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(),
                        LoginResponse.class);
    
        return response.token();
    }

    @Test
    void shouldRejectDeactivatingAnotherCustomer() throws Exception {

        register(
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!"));

        CustomerResponse john = register(
                new CustomerRequest(
                        "John",
                        "Smith",
                        null,
                        "john@example.com",
                        "456 Queen Street",
                        "Password123!"));

        String token = login(
                "jane@example.com",
                "Password123!");

        mockMvc.perform(delete("/api/customers/" + john.id())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLoginForInactiveCustomer() throws Exception {

        CustomerResponse customer = register(
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!"));

        String token = login(
                "jane@example.com",
                "Password123!");

        mockMvc.perform(delete("/api/customers/" + customer.id())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest(
                                "jane@example.com",
                                "Password123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                    .value("Customer account is inactive"));;
    }


}
