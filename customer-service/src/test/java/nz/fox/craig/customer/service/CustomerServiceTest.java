package nz.fox.craig.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.exception.CustomerNotFoundException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
	private static final String PASSWORD =
        "$2a$10$8xukrp03uk4k91AEt1BFKO.BQLwynIn3oOIn/Dqv4dCNsp6X0foe.";
	private static final String ENTERED_PASSWORD = "Password123!";

	@Mock
	private CustomerRepository customerRepository;

	@Mock 
	private PasswordEncoder passwordEncoder;


	@InjectMocks
	private CustomerService customerService;

	private final UUID CUSTOMER_ID = UUID.randomUUID();
	private final UUID CUSTOMER_ID_2 = UUID.randomUUID();
	private final UUID UNKNOWN_CUSTOMER = UUID.randomUUID();
	

	@Test
	void createCustomer() {
		CustomerRequest request = new CustomerRequest("Jane", "Doe", "Jo",
				"jane@example.com", "123 Main St", ENTERED_PASSWORD);
		Customer saved = Customer.builder()
				.id(CUSTOMER_ID)
				.firstName("Jane")
				.lastName("Doe")
				.preferredName("Jo")
				.email("jane@example.com")
				.address("123 Main St")
				.password(PASSWORD)
				.build();

		when(customerRepository.save(any(Customer.class))).thenReturn(saved);
		when(passwordEncoder.encode(ENTERED_PASSWORD))
    		.thenReturn(PASSWORD);

		CustomerResponse response = customerService.createCustomer(request);

		assertThat(response.id()).isEqualTo(CUSTOMER_ID);
		assertThat(response.firstName()).isEqualTo("Jane");
		assertThat(response.lastName()).isEqualTo("Doe");
		assertThat(response.displayName()).isEqualTo("Jo");
		assertThat(response.email()).isEqualTo("jane@example.com");
		assertThat(response.address()).isEqualTo("123 Main St");
		verify(customerRepository).save(any(Customer.class));
	}

	@Test
	void createCustomerWithNoPreferredName() {
		CustomerRequest request = new CustomerRequest("Jane", "Doe", null,
				"jane@example.com", "123 Main St", "Password123!");
		Customer saved = Customer.builder()
				.id(CUSTOMER_ID)
				.firstName("Jane")
				.lastName("Doe")
				.preferredName(null)
				.email("jane@example.com")
				.address("123 Main St")
				.password(PASSWORD)
				.build();

		when(customerRepository.save(any(Customer.class))).thenReturn(saved);

		CustomerResponse response = customerService.createCustomer(request);

		assertThat(response.displayName()).isEqualTo("Jane");
	}

	@Test
	void getAllCustomers() {
		Customer customer1 = Customer.builder()
				.id(CUSTOMER_ID)
				.firstName("Jane")
				.lastName("Doe")
				.email("jane@example.com")
				.address("123 Main St")
				.password(PASSWORD)
				.build();
		Customer customer2 = Customer.builder()
				.id(CUSTOMER_ID_2)
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.address("456 Oak Ave")
				.password(PASSWORD)
				.build();

		when(customerRepository.findAll()).thenReturn(List.of(customer1, customer2));

		List<CustomerResponse> responses = customerService.getCustomers(null);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).firstName()).isEqualTo("Jane");
		assertThat(responses.get(1).firstName()).isEqualTo("John");
	}

	@Test
	void getCustomer() {
		Customer customer = Customer.builder()
				.id(CUSTOMER_ID)
				.firstName("Jane")
				.lastName("Doe")
				.email("jane@example.com")
				.address("123 Main St")
				.password(PASSWORD)
				.build();

		when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

		CustomerResponse response = customerService.getCustomer(CUSTOMER_ID);

		assertThat(response.id()).isEqualTo(CUSTOMER_ID);
		assertThat(response.firstName()).isEqualTo("Jane");
		assertThat(response.lastName()).isEqualTo("Doe");
	}

	@Test
	void getCustomerNotFound() {
		when(customerRepository.findById(UNKNOWN_CUSTOMER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.getCustomer(UNKNOWN_CUSTOMER))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found with id: " + UNKNOWN_CUSTOMER);
	}

	@Test
	void updateCustomer() {
		CustomerRequest request = new CustomerRequest("Jane", "Smith", null,
				"jane.smith@example.com", "456 Oak Ave", ENTERED_PASSWORD);
		Customer existing = Customer.builder()
				.id(CUSTOMER_ID)
				.firstName("Jane")
				.lastName("Doe")
				.email("jane@example.com")
				.address("123 Main St")
				.password(PASSWORD)
				.build();
		Customer updated = Customer.builder()
				.id(CUSTOMER_ID)
				.firstName("Jane Smith")
				.lastName("Smith")
				.email("jane.smith@example.com")
				.address("456 Oak Ave")
				.password(PASSWORD)
				.build();

		when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
		when(customerRepository.save(existing)).thenReturn(updated);

		CustomerResponse response = customerService.updateCustomer(CUSTOMER_ID, request);

		assertThat(response.lastName()).isEqualTo("Smith");
		assertThat(response.email()).isEqualTo("jane.smith@example.com");
		assertThat(response.address()).isEqualTo("456 Oak Ave");
	}

	@Test
	void updateCustomerNotFound() {
		CustomerRequest request = new CustomerRequest("Jane", "Smith", null,
				"jane.smith@example.com", "456 Oak Ave", "Password123!");

		when(customerRepository.findById(UNKNOWN_CUSTOMER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.updateCustomer(UNKNOWN_CUSTOMER, request))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found with id: " + UNKNOWN_CUSTOMER);
	}

}
