package nz.fox.craig.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.fox.craig.customer.controller.CustomerController;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.exception.CustomerExceptionHandler;
import nz.fox.craig.customer.exception.CustomerNotFoundException;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.service.CustomerService;

@WebMvcTest(CustomerController.class)
@Import(CustomerExceptionHandler.class)
class CustomerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CustomerService customerService;

	private final UUID CUSTOMER_ID = UUID.randomUUID();
	private final UUID CUSTOMER_ID_2 = UUID.randomUUID();
	private final UUID UNKNOWN_CUSTOMER = UUID.randomUUID();

	@Test
	void createCustomer() throws Exception {

		CustomerRequest request = new CustomerRequest("Jane Doe", "jane@example.com", "123 Main St");
		CustomerResponse response = new CustomerResponse(CUSTOMER_ID, "Jane Doe", "jane@example.com", "123 Main St", CustomerStatus.ACTIVE);

		when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/customers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$.name").value("Jane Doe"))
				.andExpect(jsonPath("$.email").value("jane@example.com"))
				.andExpect(jsonPath("$.address").value("123 Main St"));
	}

	@Test
	void createCustomerWithInvalidEmail() throws Exception {
		CustomerRequest request = new CustomerRequest("Jane Doe", "not-an-email", "123 Main St");

		mockMvc.perform(post("/api/customers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.validationErrors.email").value("Email must be a valid email address"));
	}

	@Test
	void createCustomerWithMissingFields() throws Exception {
		mockMvc.perform(post("/api/customers")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getAllCustomers() throws Exception {
		List<CustomerResponse> responses = List.of(
				new CustomerResponse(CUSTOMER_ID, "Jane Doe", "jane@example.com", "123 Main St", CustomerStatus.ACTIVE),
				new CustomerResponse(CUSTOMER_ID_2, "John Doe", "john@example.com", "456 Oak Ave", CustomerStatus.ACTIVE));

		when(customerService.getCustomers(null)).thenReturn(responses);

		mockMvc.perform(get("/api/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Jane Doe"))
				.andExpect(jsonPath("$[1].name").value("John Doe"));
	}

	@Test
	void getCustomer() throws Exception {
		CustomerResponse response = new CustomerResponse(CUSTOMER_ID, "Jane Doe", "jane@example.com", "123 Main St",  CustomerStatus.ACTIVE);

		when(customerService.getCustomer(CUSTOMER_ID)).thenReturn(response);

		mockMvc.perform(get("/api/customers/" + CUSTOMER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$.name").value("Jane Doe"));
	}

	@Test
	void getCustomerNotFound() throws Exception {
		when(customerService.getCustomer(UNKNOWN_CUSTOMER)).thenThrow(new CustomerNotFoundException(UNKNOWN_CUSTOMER));

		mockMvc.perform(get("/api/customers/" + UNKNOWN_CUSTOMER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Customer not found with id: " + UNKNOWN_CUSTOMER));
	}

	@Test
	void updateCustomer() throws Exception {
		CustomerRequest request = new CustomerRequest("Jane Smith", "jane.smith@example.com", "456 Oak Ave");
		CustomerResponse response = new CustomerResponse(CUSTOMER_ID, "Jane Smith", "jane.smith@example.com",
				"456 Oak Ave",  CustomerStatus.ACTIVE);

		when(customerService.updateCustomer(eq(CUSTOMER_ID), any(CustomerRequest.class))).thenReturn(response);

		mockMvc.perform(put("/api/customers/" + CUSTOMER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Jane Smith"))
				.andExpect(jsonPath("$.email").value("jane.smith@example.com"));
	}

	@Nested
	class CustomerExists {

		@Test
		void shouldReturnOkWhenCustomerExists() throws Exception {
			UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

			when(customerService.getCustomer(customerId))
					.thenReturn(new CustomerResponse(
							customerId,
							"Alice Smith",
							"alice.smith@example.com",
							"12 Queen Street, Auckland",
							CustomerStatus.ACTIVE
					));

			mockMvc.perform(head("/api/customers/{id}", customerId))
					.andExpect(status().isOk());

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
	}

}
