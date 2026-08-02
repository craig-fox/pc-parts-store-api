package nz.fox.craig.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "this-is-a-very-long-secret-key-for-testing-only-1234567890"
                    .getBytes(StandardCharsets.UTF_8));

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                SECRET);
    }

    @Test
    void shouldValidateToken() {

        String token = JwtTestFactory.createToken(
                CUSTOMER_ID,
                EMAIL,
                SECRET,
                Duration.ofHours(1));

        assertThat(jwtService.isTokenValid(token))
                .isTrue();
    }

    @Test
    void shouldExtractCustomerId() {

        String token = JwtTestFactory.createToken(
                CUSTOMER_ID,
                EMAIL,
                SECRET,
                Duration.ofHours(1));

        assertThat(jwtService.extractCustomerId(token))
                .isEqualTo(CUSTOMER_ID);
    }

    @Test
    void shouldExtractEmail() {

        String token = JwtTestFactory.createToken(
                CUSTOMER_ID,
                EMAIL,
                SECRET,
                Duration.ofHours(1));

        assertThat(jwtService.extractEmail(token))
                .isEqualTo(EMAIL);
    }

    @Test
    void shouldReturnFalseForInvalidToken() {

        assertThat(jwtService.isTokenValid("not-a-jwt"))
                .isFalse();
    }

    @Test
    void shouldReturnFalseForExpiredToken() {

        String token = JwtTestFactory.createToken(
                CUSTOMER_ID,
                EMAIL,
                SECRET,
                Duration.ofSeconds(-1));

        assertThat(jwtService.isTokenValid(token))
                .isFalse();
    }

    @Test
    void shouldThrowWhenExtractingCustomerIdFromInvalidToken() {

        assertThatThrownBy(() -> jwtService.extractCustomerId("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldThrowWhenExtractingEmailFromInvalidToken() {

        assertThatThrownBy(() -> jwtService.extractEmail("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}