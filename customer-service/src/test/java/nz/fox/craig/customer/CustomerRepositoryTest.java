package nz.fox.craig.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CustomerRepositoryTest {

	@Autowired
	private CustomerRepository customerRepository;

	@Test
	void saveAndFindById() {
		Customer customer = Customer.builder()
        .name("Jane Doe")
        .email("jane@example.com")
        .address("123 Main St")
        .build();
	
		Customer saved = customerRepository.save(customer);

		assertThat(saved.getId()).isNotNull();
		assertThat(customerRepository.findById(saved.getId()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getName()).isEqualTo("Jane Doe");
					assertThat(found.getEmail()).isEqualTo("jane@example.com");
					assertThat(found.getAddress()).isEqualTo("123 Main St");
				});
	}

	@Test
	void findAllCustomers() {
		customerRepository.save(Customer.builder()
				.name("Jane Doe")
				.email("jane@example.com")
				.address("123 Main St")
				.build());
		customerRepository.save(Customer.builder()
				.name("John Doe")
				.email("john@example.com")
				.address("456 Oak Ave")
				.build());

		assertThat(customerRepository.findAll()).hasSize(2);
	}

	@Test
	void updateExistingCustomer() {
		Customer customer = customerRepository.save(Customer.builder()
				.name("Jane Doe")
				.email("jane@example.com")
				.address("123 Main St")
				.build());

		customer.setName("Jane Smith");
		customer.setEmail("jane.smith@example.com");
		customer.setAddress("456 Oak Ave");

		Customer updated = customerRepository.save(customer);

		assertThat(updated.getName()).isEqualTo("Jane Smith");
		assertThat(updated.getEmail()).isEqualTo("jane.smith@example.com");
		assertThat(updated.getAddress()).isEqualTo("456 Oak Ave");
	}

}
