package nz.fox.craig.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CustomerRepositoryTest extends AbstractPostgresTest{

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
					assertThat(found.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
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

	@Test
	void findByStatusReturnsOnlyMatchingCustomers() {

		customerRepository.save(Customer.builder()
				.name("Jane")
				.email("jane@example.com")
				.address("123 Main St")
				.status(CustomerStatus.ACTIVE)
				.build());

		customerRepository.save(Customer.builder()
				.name("John")
				.email("john@example.com")
				.address("456 Oak Ave")
				.status(CustomerStatus.INACTIVE)
				.build());

		assertThat(customerRepository.findByStatus(CustomerStatus.ACTIVE))
				.hasSize(1);

		assertThat(customerRepository.findByStatus(CustomerStatus.INACTIVE))
				.hasSize(1);
	}

}
