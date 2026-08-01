package nz.fox.craig.customer.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";

    private static final String PASSWORD =
            "$2a$10$8xukrp03uk4k91AEt1BFKO.BQLwynIn3oOIn/Dqv4dCNsp6X0foe.";

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                Base64.getEncoder().encodeToString(
                        "this-is-a-very-long-secret-key-for-testing-only-1234567890"
                                .getBytes(StandardCharsets.UTF_8)));

        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpiration",
                3_600_000L);
    }

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
    void shouldGenerateToken() {
        String token = jwtService.generateToken(customer());
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldExtractCustomerId() {
        String token = jwtService.generateToken(customer());
        UUID id = jwtService.extractCustomerId(token);
        assertThat(id).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void shouldExtractEmail() {
        String token = jwtService.generateToken(customer());
        String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo(EMAIL);
    }

    @Test
    void shouldValidateToken() {
        Customer customer = customer();
        String token = jwtService.generateToken(customer);
        assertThat(jwtService.isTokenValid(token))
                .isTrue();
    }

    @Test
    void shouldGenerateValidToken() {
        Customer customer = customer();

        String token = jwtService.generateToken(customer);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }
}
