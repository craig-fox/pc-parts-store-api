package nz.fox.craig.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nz.fox.craig.security.config.JwtProperties;

class TokenServiceTest {

    private static final String SECRET = "VGhpc0lzQVN1ZmZpY2llbnRMb25nU2VjcmV0S2V5Rm9ySldU";

    private static final Duration EXPIRATION = Duration.ofHours(1);

    private TokenService tokenService;

    private final JwtProperties jwtProperties = new JwtProperties(SECRET, EXPIRATION);

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtProperties);
    }

    @Test
    void shouldGenerateValidToken() {
        String token =
                tokenService.generateToken(SampleAuthenticatedUsers.authenticatedCustomerUser());
        assertThat(token).isNotBlank();
        assertThat(tokenService.isTokenValid(token)).isTrue();
    }

    @Test
    void shouldExtractCustomerId() {
        UUID customerId = UUID.randomUUID();
        String token =
                tokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser(customerId));

        assertThat(tokenService.extractCustomerId(token)).isEqualTo(customerId);
    }

    @Test
    void shouldExtractEmail() {
        String token =
                tokenService.generateToken(SampleAuthenticatedUsers.authenticatedCustomerUser());

        assertThat(tokenService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(tokenService.isTokenValid("not-a-valid-token")).isFalse();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {

        String token =
                tokenService.generateToken(SampleAuthenticatedUsers.authenticatedCustomerUser());

        tokenService = new TokenService(new JwtProperties("QW5vdGhlclZlcnlMb25nU2VjcmV0S2V5Rm9ySldU", EXPIRATION));

        assertThat(tokenService.isTokenValid(token)).isFalse();
    }

    @Test
    void shouldThrowWhenExtractingCustomerIdFromInvalidToken() {
        assertThatThrownBy(() -> tokenService.extractCustomerId("not-a-valid-token"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldThrowWhenExtractingEmailFromInvalidToken() {
        assertThatThrownBy(() -> tokenService.extractEmail("not-a-valid-token"))
                .isInstanceOf(RuntimeException.class);
    }
}
