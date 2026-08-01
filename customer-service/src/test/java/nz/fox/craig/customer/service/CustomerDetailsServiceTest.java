package nz.fox.craig.customer.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;
import nz.fox.craig.customer.security.CustomerUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerDetailsServiceTest {
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";
    private static final String PASSWORD =
            "$2a$10$8xukrp03uk4k91AEt1BFKO.BQLwynIn3oOIn/Dqv4dCNsp6X0foe.";

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerDetailsService customerDetailsService;

    private Customer customer() {
        return Customer.builder()
                .id(CUSTOMER_ID)
                .firstName("Jane")
                .lastName("Doe")
                .email(EMAIL)
                .password(PASSWORD)
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldLoadUserByUsername() {

        Customer customer = customer();

        when(customerRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(customer));

        UserDetails user =
                customerDetailsService.loadUserByUsername(EMAIL);

        assertThat(user).isInstanceOf(CustomerUserDetails.class);

        CustomerUserDetails details = (CustomerUserDetails) user;

        assertThat(details.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getPassword()).isEqualTo(PASSWORD);

        verify(customerRepository).findByEmail(EMAIL);
    }

    @Test
    void shouldThrowWhenUsernameNotFound() {

        when(customerRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                customerDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(EMAIL);

        verify(customerRepository).findByEmail(EMAIL);
    }

    @Test
    void shouldLoadUserById() {

        Customer customer = customer();

        when(customerRepository.findById(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));

        CustomerUserDetails user =
                customerDetailsService.loadUserById(CUSTOMER_ID);

        assertThat(user.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(user.getUsername()).isEqualTo(EMAIL);

        verify(customerRepository).findById(CUSTOMER_ID);
    }

    @Test
    void shouldThrowWhenCustomerIdNotFound() {

        when(customerRepository.findById(CUSTOMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                customerDetailsService.loadUserById(CUSTOMER_ID))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(CUSTOMER_ID.toString());

        verify(customerRepository).findById(CUSTOMER_ID);
    }

}
