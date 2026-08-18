package nz.fox.craig.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final String SECRET = "VGhpc0lzQVN1ZmZpY2llbnRMb25nU2VjcmV0S2V5Rm9ySldU";

    private static final long EXPIRATION = 3600000L;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();

        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "jwtExpiration", EXPIRATION);
    }

    @Test
    void shouldGenerateValidToken() {
        String token =
                tokenService.generateToken(SampleAuthenticatedUsers.authenticatedCustomerUser());
        ;

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

        ReflectionTestUtils.setField(
                tokenService, "secret", "QW5vdGhlclZlcnlMb25nU2VjcmV0S2V5Rm9ySldU");

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
