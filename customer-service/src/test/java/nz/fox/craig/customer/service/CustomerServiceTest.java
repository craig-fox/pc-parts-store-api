package nz.fox.craig.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nz.fox.craig.customer.dto.CustomerAuthenticationResponse;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.exception.CustomerAlreadyExistsException;
import nz.fox.craig.customer.exception.CustomerNotFoundException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;
import nz.fox.craig.customer.security.SecurityService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    private static final String PASSWORD =
            "$2a$10$8xukrp03uk4k91AEt1BFKO.BQLwynIn3oOIn/Dqv4dCNsp6X0foe.";
    private static final String ENTERED_PASSWORD = "Password123!";

    @Mock private CustomerRepository customerRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private SecurityService securityService;

    @InjectMocks private CustomerService customerService;

    private final UUID CUSTOMER_ID = UUID.randomUUID();
    private final UUID CUSTOMER_ID_2 = UUID.randomUUID();
    private final UUID UNKNOWN_CUSTOMER = UUID.randomUUID();

    @Nested
    class RegisterCustomer {
        @Test
        void registersCustomer() {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane",
                            "Doe",
                            "Jo",
                            "jane@example.com",
                            "123 Main St",
                            ENTERED_PASSWORD);
            Customer saved =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .preferredName("Jo")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .build();

            when(customerRepository.save(any(Customer.class))).thenReturn(saved);
            when(passwordEncoder.encode(ENTERED_PASSWORD)).thenReturn(PASSWORD);

            CustomerResponse response = customerService.registerCustomer(request);

            assertThat(response.id()).isEqualTo(CUSTOMER_ID);
            assertThat(response.firstName()).isEqualTo("Jane");
            assertThat(response.lastName()).isEqualTo("Doe");
            assertThat(response.displayName()).isEqualTo("Jo");
            assertThat(response.email()).isEqualTo("jane@example.com");
            assertThat(response.address()).isEqualTo("123 Main St");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        void registersCustomerWithNoPreferredName() {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane", "Doe", null, "jane@example.com", "123 Main St", "Password123!");
            Customer saved =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .preferredName(null)
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .build();

            when(customerRepository.save(any(Customer.class))).thenReturn(saved);

            CustomerResponse response = customerService.registerCustomer(request);

            assertThat(response.displayName()).isEqualTo("Jane");
        }

        @Test
        void rejectsExistingEmail() {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane",
                            "Doe",
                            "Jo",
                            "jane@example.com",
                            "123 Main St",
                            ENTERED_PASSWORD);

            when(customerRepository.findByEmail(request.email()))
                    .thenReturn(Optional.of(Customer.builder().email(request.email()).build()));

            assertThatThrownBy(() -> customerService.registerCustomer(request))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessage("Customer already exists with email: jane@example.com");

            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    @Nested
    class GetCustomer {
        @Test
        void getsAllCustomers() {
            Customer customer1 =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .build();
            Customer customer2 =
                    Customer.builder()
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
        void getACustomer() {
            Customer customer =
                    Customer.builder()
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

            verify(securityService).verifyCurrentUser(CUSTOMER_ID);
        }

        @Test
        void customerIsNotFound() {
            when(customerRepository.findById(UNKNOWN_CUSTOMER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deactivateCustomer(UNKNOWN_CUSTOMER))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with id: " + UNKNOWN_CUSTOMER);

            verify(securityService).verifyCurrentUser(UNKNOWN_CUSTOMER);
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        void getCustomersByStatus() {
            Customer activeCustomer =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .status(CustomerStatus.ACTIVE)
                            .build();

            when(customerRepository.findByStatus(CustomerStatus.ACTIVE))
                    .thenReturn(List.of(activeCustomer));

            List<CustomerResponse> responses = customerService.getCustomers(CustomerStatus.ACTIVE);

            assertThat(responses)
                    .singleElement()
                    .satisfies(
                            response -> {
                                assertThat(response.id()).isEqualTo(CUSTOMER_ID);
                                assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
                            });

            verify(customerRepository).findByStatus(CustomerStatus.ACTIVE);
        }

        @Test
        void getCustomerByEmail() {
            Customer customer =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .preferredName("Jo")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .status(CustomerStatus.ACTIVE)
                            .build();

            when(customerRepository.findByEmail("jane@example.com"))
                    .thenReturn(Optional.of(customer));

            CustomerAuthenticationResponse response =
                    customerService.getCustomerByEmail("jane@example.com");

            assertThat(response.id()).isEqualTo(CUSTOMER_ID);
            assertThat(response.email()).isEqualTo("jane@example.com");
            assertThat(response.password()).isEqualTo(PASSWORD);
            assertThat(response.active()).isTrue();
            assertThat(response.firstName()).isEqualTo("Jane");
            assertThat(response.preferredName()).isEqualTo("Jo");
        }

        @Test
        void getCustomerByEmailNotFound() {
            when(customerRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerByEmail("unknown@example.com"))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with email: unknown@example.com");
        }

        @Test
        void shouldThrowWhenCustomerDoesNotExist() {
                UUID customerId = UUID.randomUUID();

                when(customerRepository.findById(customerId))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> customerService.getCustomer(customerId))
                        .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    class UpdateCustomer {
        @Test
        void updatesCustomer() {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane",
                            "Smith",
                            null,
                            "jane.smith@example.com",
                            "456 Oak Ave",
                            ENTERED_PASSWORD);
            Customer existing =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .build();
            Customer updated =
                    Customer.builder()
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
            verify(securityService).verifyCurrentUser(CUSTOMER_ID);
        }

        @Test
        void customerIsNotFound() {
            CustomerRequest request =
                    new CustomerRequest(
                            "Jane",
                            "Smith",
                            null,
                            "jane.smith@example.com",
                            "456 Oak Ave",
                            "Password123!");

            when(customerRepository.findById(UNKNOWN_CUSTOMER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(UNKNOWN_CUSTOMER, request))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with id: " + UNKNOWN_CUSTOMER);
        }
    }

    @Nested
    class ActivateCustomer {

        @Test
        void activateCustomer() {
            Customer customer =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .status(CustomerStatus.INACTIVE)
                            .build();

            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

            customerService.activateCustomer(CUSTOMER_ID);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            verify(customerRepository).save(customer);
        }

        @Test
        void activateAlreadyActiveCustomer() {
            Customer customer =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .status(CustomerStatus.ACTIVE)
                            .build();

            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> customerService.activateCustomer(CUSTOMER_ID))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessage("Customer already exists with email: jane@example.com");

            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    @Nested
    class DeactivateCustomer {

        @Test
        void deactivatesCustomer() {
            Customer customer =
                    Customer.builder()
                            .id(CUSTOMER_ID)
                            .firstName("Jane")
                            .lastName("Doe")
                            .email("jane@example.com")
                            .address("123 Main St")
                            .password(PASSWORD)
                            .status(CustomerStatus.ACTIVE)
                            .build();

            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

            customerService.deactivateCustomer(CUSTOMER_ID);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
            verify(customerRepository).save(customer);
            verify(securityService).verifyCurrentUser(CUSTOMER_ID);
        }

        @Test
        void customerIsNotFound() {
            when(customerRepository.findById(UNKNOWN_CUSTOMER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deactivateCustomer(UNKNOWN_CUSTOMER))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with id: " + UNKNOWN_CUSTOMER);
        }
    }
}
