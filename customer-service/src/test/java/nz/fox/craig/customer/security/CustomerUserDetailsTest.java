package nz.fox.craig.customer.security;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CustomerUserDetailsTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";
    private static final String PASSWORD =
            "$2a$10$8xukrp03uk4k91AEt1BFKO.BQLwynIn3oOIn/Dqv4dCNsp6X0foe.";

    @Test
    void shouldReturnCustomerProperties() {

        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .firstName("Jane")
                .lastName("Doe")
                .email(EMAIL)
                .password(PASSWORD)
                .status(CustomerStatus.ACTIVE)
                .build();

        CustomerUserDetails userDetails =
                new CustomerUserDetails(customer);

        assertThat(userDetails.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(userDetails.getUsername()).isEqualTo(EMAIL);
        assertThat(userDetails.getPassword()).isEqualTo(PASSWORD);
        assertThat(userDetails.getFirstName()).isEqualTo("Jane");
        assertThat(userDetails.getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldReturnCustomerRole() {

        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .email(EMAIL)
                .password(PASSWORD)
                .status(CustomerStatus.ACTIVE)
                .build();

        CustomerUserDetails userDetails =
                new CustomerUserDetails(customer);

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void shouldBeEnabledWhenCustomerIsActive() {

        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .email(EMAIL)
                .password(PASSWORD)
                .status(CustomerStatus.ACTIVE)
                .build();

        CustomerUserDetails userDetails =
                new CustomerUserDetails(customer);

        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void shouldNotBeEnabledWhenCustomerIsInactive() {

        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .email(EMAIL)
                .password(PASSWORD)
                .status(CustomerStatus.INACTIVE)
                .build();

        CustomerUserDetails userDetails =
                new CustomerUserDetails(customer);

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void shouldAlwaysReportAccountAsValid() {

        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .email(EMAIL)
                .password(PASSWORD)
                .status(CustomerStatus.ACTIVE)
                .build();

        CustomerUserDetails userDetails =
                new CustomerUserDetails(customer);

        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }
}
