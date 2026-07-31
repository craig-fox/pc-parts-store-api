package nz.fox.craig.customer.security;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import nz.fox.craig.customer.dto.LoginRequest;
import nz.fox.craig.customer.dto.LoginResponse;
import nz.fox.craig.customer.exception.InvalidCredentialsException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private final String EMAIL = "my.email@google.com";
    private final String LOGIN_PASSWORD = "123ABC$";

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private Customer activeCustomer() {
        return Customer.builder()
                .id(CUSTOMER_ID)
                .firstName("Jane")
                .preferredName("Jo")
                .lastName("Doe")
                .email("jane@example.com")
                .password("encoded-password")
                .address("123 Main Street")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldLoginSuccessfully() {
        final LoginRequest request = new LoginRequest(
                EMAIL,
                LOGIN_PASSWORD);

        final Customer customer = activeCustomer();

        when(customerRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(customer));

        when(passwordEncoder.matches(
                request.password(),
                customer.getPassword()))
                .thenReturn(true);

        when(jwtService.generateToken(customer))
                .thenReturn("jwt-token");

        final LoginResponse response = authenticationService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");

        verify(customerRepository).findByEmail(request.email());
        verify(passwordEncoder).matches(
                request.password(),
                customer.getPassword());
        verify(jwtService).generateToken(customer);
    }

    @Test
    void shouldThrowWhenEmailDoesNotExist() {
        final LoginRequest request = new LoginRequest(
                "unknown@example.com", LOGIN_PASSWORD);

        when(customerRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any());
    }

    @Test
    void shouldThrowWhenPasswordIsIncorrect() {
        final LoginRequest request = new LoginRequest(
                EMAIL, "wrong-password");

        final Customer customer = activeCustomer();

        when(customerRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(customer));

        when(passwordEncoder.matches(
                request.password(),
                customer.getPassword()))
                .thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(jwtService, never())
                .generateToken(any());
    }

    @Test
    void shouldThrowWhenCustomerIsInactive() {
        final LoginRequest request = new LoginRequest(
                EMAIL,
                LOGIN_PASSWORD);

        final Customer customer = activeCustomer();
        customer.setStatus(CustomerStatus.INACTIVE);

        when(customerRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any());
    }

}
