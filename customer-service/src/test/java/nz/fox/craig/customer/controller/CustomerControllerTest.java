package nz.fox.craig.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.customer.dto.CustomerAuthenticationResponse;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.exception.CustomerAlreadyExistsException;
import nz.fox.craig.customer.exception.CustomerExceptionHandler;
import nz.fox.craig.customer.exception.CustomerNotFoundException;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.service.CustomerService;
import nz.fox.craig.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CustomerExceptionHandler.class)
class CustomerControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CustomerService customerService;

    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID_2 = UUID.randomUUID();
    private static final UUID UNKNOWN_CUSTOMER = UUID.randomUUID();
    private static final String PASSWORD = "Password123!";

    @Nested
    class RegisterCustomer {
        @Test
        void registerCustomer() throws Exception {

            CustomerRequest request =
                    new CustomerRequest(
                            "Jane", "Doe", "Jo", "jane@example.com", "123 Main St", PASSWORD);
            when(customerService.registerCustomer(any(CustomerRequest.class)))
                    .thenReturn(JANE_RESPONSE);

            mockMvc.perform(
                            post("/api/customers")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.displayName").value("Jane"))
                    .andExpect(jsonPath("$.email").value("jane@example.com"))
                    .andExpect(jsonPath("$.address").value("123 Main St"));
        }

        @Test
        void failsWithInvalidEmail() throws Exception {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane", "Doe", null, "not-an-email", "123 Main St", PASSWORD);

            mockMvc.perform(
                            post("/api/customers")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(
                            jsonPath("$.validationErrors.email")
                                    .value("Email must be a valid email address"));
        }

        @Test
        void failsWithMissingFields() throws Exception {
            mockMvc.perform(
                            post("/api/customers")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetCustomers {
        @Test
        void getAllCustomers() throws Exception {
            List<CustomerResponse> responses = List.of(JANE_RESPONSE, JOHN_RESPONSE);

            when(customerService.getCustomers(null)).thenReturn(responses);

            mockMvc.perform(get("/api/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].firstName").value("Jane"))
                    .andExpect(jsonPath("$[0].lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].displayName").value("Jane"))
                    .andExpect(jsonPath("$[1].firstName").value("John"))
                    .andExpect(jsonPath("$[1].lastName").value("Doe"))
                    .andExpect(jsonPath("$[1].displayName").value("Jack"));
        }

        @Test
        void getCustomer() throws Exception {

            when(customerService.getCustomer(CUSTOMER_ID)).thenReturn(JANE_RESPONSE);

            mockMvc.perform(get("/api/customers/" + CUSTOMER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));
        }

        @Test
        void getCustomerNotFound() throws Exception {
            when(customerService.getCustomer(UNKNOWN_CUSTOMER))
                    .thenThrow(new CustomerNotFoundException(UNKNOWN_CUSTOMER));

            mockMvc.perform(get("/api/customers/" + UNKNOWN_CUSTOMER))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer not found with id: " + UNKNOWN_CUSTOMER));
        }

        @Test
        void getsCustomersByStatus() throws Exception {
            when(customerService.getCustomers(CustomerStatus.ACTIVE))
                    .thenReturn(List.of(JOHN_RESPONSE, INACTIVE_JANE_RESPONSE));

            mockMvc.perform(get("/api/customers").param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(CUSTOMER_ID_2.toString()))
                    .andExpect(jsonPath("$[0].firstName").value("John"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));

            verify(customerService).getCustomers(CustomerStatus.ACTIVE);
        }
    }

    @Nested
    class GetCustomerByEmail {
        @Test
        void getCustomerByEmail() throws Exception {
            CustomerAuthenticationResponse response =
                    new CustomerAuthenticationResponse(
                            CUSTOMER_ID, "jane@example.com", "hashed-password", true, "Jane", "Jo");

            when(customerService.getCustomerByEmail("jane@example.com")).thenReturn(response);

            mockMvc.perform(get("/api/customers/email/{email}", "jane@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                    .andExpect(jsonPath("$.email").value("jane@example.com"))
                    .andExpect(jsonPath("$.password").value("hashed-password"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.preferredName").value("Jo"));

            verify(customerService).getCustomerByEmail("jane@example.com");
        }

        @Test
        void getCustomerByEmailNotFound() throws Exception {
            when(customerService.getCustomerByEmail("unknown@example.com"))
                    .thenThrow(new CustomerNotFoundException("unknown@example.com"));

            mockMvc.perform(get("/api/customers/email/{email}", "unknown@example.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "Customer not found with email: "
                                                    + "unknown@example.com"));

            verify(customerService).getCustomerByEmail("unknown@example.com");
        }
    }

    @Nested
    class UpdateCustomer {
        @Test
        void updateCustomer() throws Exception {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane",
                            "Smith",
                            null,
                            "jane.smith@example.com",
                            "456 Oak Ave",
                            PASSWORD);
            CustomerResponse response =
                    new CustomerResponse(
                            CUSTOMER_ID,
                            "Jane",
                            "Smith",
                            "Jane",
                            "jane.smith@example.com",
                            "456 Oak Ave",
                            CustomerStatus.ACTIVE);

            when(customerService.updateCustomer(eq(CUSTOMER_ID), any(CustomerRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(
                            put("/api/customers/" + CUSTOMER_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.email").value("jane.smith@example.com"));
        }

        @Test
        void updateCustomerNotFound() throws Exception {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane",
                            "Smith",
                            null,
                            "jane.smith@example.com",
                            "456 Oak Ave",
                            PASSWORD);

            doThrow(new CustomerNotFoundException(UNKNOWN_CUSTOMER))
                    .when(customerService)
                    .updateCustomer(eq(UNKNOWN_CUSTOMER), any(CustomerRequest.class));

            mockMvc.perform(
                            put("/api/customers/" + UNKNOWN_CUSTOMER)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer not found with id: " + UNKNOWN_CUSTOMER));

            verify(customerService)
                    .updateCustomer(eq(UNKNOWN_CUSTOMER), any(CustomerRequest.class));
        }
    }

    @Nested
    class CustomerExists {

        @Test
        void shouldReturnOkWhenCustomerExists() throws Exception {
            UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            when(customerService.getCustomer(customerId))
                    .thenReturn(
                            new CustomerResponse(
                                    customerId,
                                    "Alice Smith",
                                    "Smith",
                                    "Alice",
                                    "alice.smith@example.com",
                                    "12 Queen Street, Auckland",
                                    CustomerStatus.ACTIVE));

            mockMvc.perform(head("/api/customers/{id}", customerId)).andExpect(status().isOk());

            verify(customerService).getCustomer(customerId);
        }

        @Test
        void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
            UUID customerId = UUID.randomUUID();

            when(customerService.getCustomer(customerId))
                    .thenThrow(new CustomerNotFoundException(customerId));

            mockMvc.perform(head("/api/customers/{id}", customerId))
                    .andExpect(status().isNotFound());

            verify(customerService).getCustomer(customerId);
        }

        @Test
        void failsWhenCustomerAlreadyExists() throws Exception {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane", "Doe", "Jo", "jane@example.com", "123 Main St", PASSWORD);

            when(customerService.registerCustomer(any(CustomerRequest.class)))
                    .thenThrow(new CustomerAlreadyExistsException("jane@example.com"));

            mockMvc.perform(
                            post("/api/customers")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer already exists with email: jane@example.com"));

            verify(customerService).registerCustomer(any(CustomerRequest.class));
        }
    }

    @Nested
    class ActivateCustomer {

        @Test
        void activatesCustomer() throws Exception {
            mockMvc.perform(put("/api/customers/{id}/activate", CUSTOMER_ID))
                    .andExpect(status().isNoContent());

            verify(customerService).activateCustomer(CUSTOMER_ID);
        }

        @Test
        void activateCustomerNotFound() throws Exception {
            doThrow(new CustomerNotFoundException(UNKNOWN_CUSTOMER))
                    .when(customerService)
                    .activateCustomer(UNKNOWN_CUSTOMER);

            mockMvc.perform(put("/api/customers/{id}/activate", UNKNOWN_CUSTOMER))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer not found with id: " + UNKNOWN_CUSTOMER));

            verify(customerService).activateCustomer(UNKNOWN_CUSTOMER);
        }

        @Test
        void activateAlreadyActiveCustomer() throws Exception {
            doThrow(new CustomerAlreadyExistsException("jane@example.com"))
                    .when(customerService)
                    .activateCustomer(CUSTOMER_ID);

            mockMvc.perform(put("/api/customers/{id}/activate", CUSTOMER_ID))
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer already exists with email: jane@example.com"));

            verify(customerService).activateCustomer(CUSTOMER_ID);
        }
    }

    @Nested
    class DeactivateCustomer {

        @Test
        void deactivatesCustomer() throws Exception {
            mockMvc.perform(delete("/api/customers/{id}", CUSTOMER_ID))
                    .andExpect(status().isNoContent());

            verify(customerService).deactivateCustomer(CUSTOMER_ID);
        }

        @Test
        void deactivateCustomerNotFound() throws Exception {
            doThrow(new CustomerNotFoundException(UNKNOWN_CUSTOMER))
                    .when(customerService)
                    .deactivateCustomer(UNKNOWN_CUSTOMER);

            mockMvc.perform(delete("/api/customers/{id}", UNKNOWN_CUSTOMER))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Customer not found with id: " + UNKNOWN_CUSTOMER));

            verify(customerService).deactivateCustomer(UNKNOWN_CUSTOMER);
        }
    }

    private static final CustomerResponse JANE_RESPONSE =
            new CustomerResponse(
                    CUSTOMER_ID,
                    "Jane",
                    "Doe",
                    "Jane",
                    "jane@example.com",
                    "123 Main St",
                    CustomerStatus.ACTIVE);

    private static final CustomerResponse JOHN_RESPONSE =
            new CustomerResponse(
                    CUSTOMER_ID_2,
                    "John",
                    "Doe",
                    "Jack",
                    "john@example.com",
                    "456 Oak Ave",
                    CustomerStatus.ACTIVE);

    private static final CustomerResponse INACTIVE_JANE_RESPONSE =
            new CustomerResponse(
                    CUSTOMER_ID,
                    "Jane",
                    "Doe",
                    "Jane",
                    "jane@example.com",
                    "123 Main St",
                    CustomerStatus.INACTIVE);
}
