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
import nz.fox.craig.customer.repository.CustomerRepository;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;
import nz.fox.craig.security.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerSecurityIntegrationTest extends AbstractPostgresTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldRetrieveOwnCustomer() {

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

        CustomerResponse janeResponse =
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

        CustomerResponse johnResponse =
                objectMapper.readValue(
                        mockMvc.perform(post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(johnRequest)))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        CustomerResponse.class);

        String token = generateToken(janeResponse);

        mockMvc.perform(get("/api/customers/" + johnResponse.id())
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }


    @Test
    void shouldRejectInvalidJwt() throws Exception {

        mockMvc.perform(get("/api/customers/" + UUID.randomUUID())
                .header("Authorization", "Bearer this-is-not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldRejectUpdatingAnotherCustomer() throws Exception {

        CustomerResponse janeResponse = register(
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!"));
        
        CustomerResponse johnResponse = register(
                new CustomerRequest(
                        "John",
                        "Smith",
                        null,
                        "john@example.com",
                        "456 Queen Street",
                        "Password123!"));
        
        String token = generateToken(janeResponse);
        
        CustomerRequest updateRequest = new CustomerRequest( "John Updated", "Smith", "Johnny", "john@example.com", "789 New Street", "Password123!");
        
        mockMvc.perform(put("/api/customers/" + johnResponse.id()) 
                .header("Authorization", "Bearer " + token) .contentType(MediaType.APPLICATION_JSON) 
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

    @Test
    void shouldRejectDeactivatingAnotherCustomer() throws Exception {

        CustomerResponse janeResponse = register(
                new CustomerRequest(
                        "Jane",
                        "Doe",
                        "Jo",
                        "jane@example.com",
                        "123 Main Street",
                        "Password123!"));

        CustomerResponse johnResponse = register(
                new CustomerRequest(
                        "John",
                        "Smith",
                        null,
                        "john@example.com",
                        "456 Queen Street",
                        "Password123!"));

        String token = generateToken(janeResponse);

        mockMvc.perform(delete("/api/customers/" + johnResponse.id())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }


    private String generateToken(CustomerResponse customer) {

        AuthenticatedUser user =
                new AuthenticatedUser(
                        customer.id(),
                        customer.email(),
                        Set.of(Role.ROLE_CUSTOMER));
    
        return tokenService.generateToken(user);
    }


}
