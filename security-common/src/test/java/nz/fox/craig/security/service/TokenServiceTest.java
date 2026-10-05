package nz.fox.craig.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;
import nz.fox.craig.security.config.JwtProperties;
import nz.fox.craig.security.dto.AuthenticatedUser;
import nz.fox.craig.security.dto.Role;

class TokenServiceTest {

    private static final String SECRET =
            "VGhpc0lzQVN1ZmZpY2llbnRMb25nU2VjcmV0S2V5Rm9ySldU";

    private static final Duration EXPIRATION = Duration.ofHours(1);

    private TokenService tokenService;

    private final JwtProperties jwtProperties =
            new JwtProperties(SECRET, EXPIRATION);

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtProperties);
    }

    @Test
    void shouldGenerateValidToken() {
        String token =
                tokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser());

        assertThat(token).isNotBlank();

        AuthenticatedUser user = tokenService.parseUser(token);

        assertThat(user.email()).isEqualTo("test@example.com");
        assertThat(user.roles()).containsExactly(Role.ROLE_CUSTOMER);
    }

    @Test
    void shouldParseCustomerId() {
        UUID customerId = UUID.randomUUID();

        String token =
                tokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser(customerId));

        AuthenticatedUser user = tokenService.parseUser(token);

        assertThat(user.id()).isEqualTo(customerId);
    }

    @Test
    void shouldParseEmail() {
        String token =
                tokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser());

        AuthenticatedUser user = tokenService.parseUser(token);

        assertThat(user.email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldParseRoles() {
        String token =
                tokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser());

        AuthenticatedUser user = tokenService.parseUser(token);

        assertThat(user.roles())
                .containsExactly(Role.ROLE_CUSTOMER);
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThatThrownBy(() -> tokenService.parseUser("not-a-valid-token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        String token =
                tokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser());

        tokenService =
                new TokenService(
                        new JwtProperties(
                                "QW5vdGhlclZlcnlMb25nU2VjcmV0S2V5Rm9ySldU",
                                EXPIRATION));

        assertThatThrownBy(() -> tokenService.parseUser(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectExpiredToken() {
        TokenService expiredTokenService =
                new TokenService(
                        new JwtProperties(
                                SECRET,
                                Duration.ofMillis(-1)));

        String token =
                expiredTokenService.generateToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser());

        assertThatThrownBy(() -> tokenService.parseUser(token))
                .isInstanceOf(JwtException.class);
    }
}
