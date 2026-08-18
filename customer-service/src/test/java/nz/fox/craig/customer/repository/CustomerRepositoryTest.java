package nz.fox.craig.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import nz.fox.craig.customer.fixture.CustomerTestFactory;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.test.AbstractPostgresTest;
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
            Customer saved = customerRepository.save(standardCustomer());

            assertThat(saved.getId()).isNotNull();
            assertThat(customerRepository.findById(saved.getId()))
                    .isPresent()
                    .get()
                    .satisfies(
                            found -> {
                                assertThat(found.getFirstName()).isEqualTo("Test");
                                assertThat(found.getLastName()).isEqualTo("Customer");
                                assertThat(found.getEmail()).isEqualTo("jane@example.com");
                                assertThat(found.getAddress()).isEqualTo("123 Test Street");
                                assertThat(found.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
                                assertThat(found.getPassword()).isEqualTo(HASHED_PASSWORD);
                            });
        }

        @Test
        void updateExistingCustomer() {
            Customer customer = standardCustomer();

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
            Customer jane = standardCustomer();
            Customer john = standardCustomer();
            john.setEmail("john@example.com");
            customerRepository.saveAll(List.of(jane, john));
            List<Customer> customers = customerRepository.findAll();

            assertThat(customers).extracting(Customer::getId).contains(jane.getId(), john.getId());
        }

        @Test
        void findsCustomerByEmail() {
            Customer customer = standardCustomer();
            customerRepository.save(customer);
            Optional<Customer> result = customerRepository.findByEmail("jane@example.com");

            assertThat(result)
                    .isPresent()
                    .get()
                    .satisfies(
                            found -> {
                                assertThat(found.getId()).isEqualTo(customer.getId());
                                assertThat(found.getFirstName()).isEqualTo("Test");
                                assertThat(found.getEmail()).isEqualTo("jane@example.com");
                            });
        }

        @Test
        void findsNoCustomersEmailDoesNotExist() {
            assertThat(customerRepository.findByEmail("unknown@example.com")).isEmpty();
        }

        @Test
        void findsCustomersByStatus() {
            Customer active = standardCustomer();

            Customer inactive = standardCustomer();
            inactive.setEmail("john@example.com");
            inactive.setStatus(CustomerStatus.INACTIVE);

            customerRepository.saveAll(List.of(active, inactive));


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

    private Customer standardCustomer() {
        Customer customer = CustomerTestFactory.aCustomer();
        customer.setEmail("jane@example.com");
        customer.setPassword(HASHED_PASSWORD);
        return customer;

    }
}
