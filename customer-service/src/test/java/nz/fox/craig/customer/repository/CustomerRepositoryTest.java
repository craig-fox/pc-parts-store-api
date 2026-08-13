package nz.fox.craig.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import nz.fox.craig.customer.common.AbstractPostgresTest;
import nz.fox.craig.customer.config.TestSecurityConfig;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataJpaTest
@Import(TestSecurityConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CustomerRepositoryTest extends AbstractPostgresTest {

    private static final String ENTERED_PASSWORD = "Password123!";
    private String hashed = "";

    @Autowired private CustomerRepository customerRepository;

    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void init() {
        hashed = encoder.encode(ENTERED_PASSWORD);
    }

    @Test
    void saveAndFindById() {

        Customer customer =
                Customer.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .preferredName("Jenny")
                        .email("jane@example.com")
                        .address("123 Main St")
                        .password(hashed)
                        .build();

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(customerRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(
                        found -> {
                            assertThat(found.getFirstName()).isEqualTo("Jane");
                            assertThat(found.getLastName()).isEqualTo("Doe");
                            assertThat(found.getEmail()).isEqualTo("jane@example.com");
                            assertThat(found.getAddress()).isEqualTo("123 Main St");
                            assertThat(found.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
                            assertTrue(encoder.matches(hashed, found.getPassword()));
                        });
    }

    @Test
    void findAllCustomers() {
        customerRepository.save(
                Customer.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .email("jane@example.com")
                        .address("123 Main St")
                        .password(hashed)
                        .build());
        customerRepository.save(
                Customer.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .address("456 Oak Ave")
                        .password(hashed)
                        .build());

        assertThat(customerRepository.findAll()).hasSize(12);
    }

    @Test
    void updateExistingCustomer() {
        Customer customer =
                customerRepository.save(
                        Customer.builder()
                                .firstName("Jane")
                                .lastName("Doe")
                                .email("jane@example.com")
                                .address("123 Main St")
                                .build());

        customer.setLastName("Smith");
        customer.setEmail("jane.smith@example.com");
        customer.setAddress("456 Oak Ave");

        Customer updated = customerRepository.save(customer);

        assertThat(updated.getLastName()).isEqualTo("Smith");
        assertThat(updated.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(updated.getAddress()).isEqualTo("456 Oak Ave");
    }

    @Test
    void findByStatusReturnsOnlyMatchingCustomers() {

        customerRepository.save(
                Customer.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .email("jane@example.com")
                        .address("123 Main St")
                        .status(CustomerStatus.ACTIVE)
                        .password(hashed)
                        .build());

        customerRepository.save(
                Customer.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .address("456 Oak Ave")
                        .status(CustomerStatus.INACTIVE)
                        .password(hashed)
                        .build());

        assertThat(customerRepository.findByStatus(CustomerStatus.ACTIVE)).hasSize(9);

        assertThat(customerRepository.findByStatus(CustomerStatus.INACTIVE)).hasSize(2);
    }
}
