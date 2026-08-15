package nz.fox.craig.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import nz.fox.craig.customer.common.AbstractPostgresTest;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CustomerRepositoryTest extends AbstractPostgresTest {

    private static final String HASHED_PASSWORD =
            "$2a$10$8xukrp03uk4k91AEt1BFKO.BQLwynIn3oOIn/Dqv4dCNsp6X0foe.";

    @Autowired private CustomerRepository customerRepository;

    @Nested
    class SaveAndUpdateCustomer {
        @Test
        void saveAndFindById() {

            Customer customer =
                    Customer.builder()
                            .firstName("Jane")
                            .lastName("Doe")
                            .preferredName("Jenny")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(HASHED_PASSWORD)
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
                                assertThat(found.getPassword()).isEqualTo(HASHED_PASSWORD);
                            });
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
                                    .password(HASHED_PASSWORD)
                                    .build());

            customer.setLastName("Smith");
            customer.setEmail("jane.smith@example.com");
            customer.setAddress("456 Oak Ave");

            Customer updated = customerRepository.save(customer);

            assertThat(updated.getLastName()).isEqualTo("Smith");
            assertThat(updated.getEmail()).isEqualTo("jane.smith@example.com");
            assertThat(updated.getAddress()).isEqualTo("456 Oak Ave");
        }
    }

    @Nested
    class FindCustomers {
        @Test
        void findsAllCustomers() {
            Customer jane =
                    customerRepository.save(
                            Customer.builder()
                                    .firstName("Jane")
                                    .lastName("Doe")
                                    .email("jane@example.com")
                                    .address("123 Main St")
                                    .password(HASHED_PASSWORD)
                                    .build());

            Customer john =
                    customerRepository.save(
                            Customer.builder()
                                    .firstName("John")
                                    .lastName("Doe")
                                    .email("john@example.com")
                                    .address("456 Oak Ave")
                                    .password(HASHED_PASSWORD)
                                    .build());

            List<Customer> customers = customerRepository.findAll();

            assertThat(customers).extracting(Customer::getId).contains(jane.getId(), john.getId());
        }

        @Test
        void findsCustomerByEmail() {
            Customer customer =
                    customerRepository.save(
                            Customer.builder()
                                    .firstName("Jane")
                                    .lastName("Doe")
                                    .email("jane@example.com")
                                    .address("123 Main St")
                                    .password(HASHED_PASSWORD)
                                    .build());

            Optional<Customer> result = customerRepository.findByEmail("jane@example.com");

            assertThat(result)
                    .isPresent()
                    .get()
                    .satisfies(
                            found -> {
                                assertThat(found.getId()).isEqualTo(customer.getId());
                                assertThat(found.getFirstName()).isEqualTo("Jane");
                                assertThat(found.getEmail()).isEqualTo("jane@example.com");
                            });
        }

        @Test
        void findsNoCustomersEmailDoesNotExist() {
            assertThat(customerRepository.findByEmail("unknown@example.com")).isEmpty();
        }

        @Test
        void findsCustomersByStatus() {
            Customer active =
                    customerRepository.save(
                            Customer.builder()
                                    .firstName("Jane")
                                    .lastName("Doe")
                                    .email("jane@example.com")
                                    .address("123 Main St")
                                    .password(HASHED_PASSWORD)
                                    .status(CustomerStatus.ACTIVE)
                                    .build());

            Customer inactive =
                    customerRepository.save(
                            Customer.builder()
                                    .firstName("John")
                                    .lastName("Doe")
                                    .email("john@example.com")
                                    .address("456 Oak Ave")
                                    .password(HASHED_PASSWORD)
                                    .status(CustomerStatus.INACTIVE)
                                    .build());

            List<Customer> activeCustomers = customerRepository.findByStatus(CustomerStatus.ACTIVE);

            List<Customer> inactiveCustomers =
                    customerRepository.findByStatus(CustomerStatus.INACTIVE);

            assertThat(activeCustomers)
                    .extracting(Customer::getId)
                    .contains(active.getId())
                    .doesNotContain(inactive.getId());

            assertThat(inactiveCustomers)
                    .extracting(Customer::getId)
                    .contains(inactive.getId())
                    .doesNotContain(active.getId());
        }
    }
}
