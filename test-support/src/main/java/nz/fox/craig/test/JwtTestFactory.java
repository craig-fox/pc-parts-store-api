package nz.fox.craig.test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class JwtTestFactory {

    private JwtTestFactory() {}

    public static String createToken(
        UUID customerId,
        String email,
        String secret,
        Duration expiration,
        Set<String> roles) {
    
    Instant now = Instant.now();

    return Jwts.builder()
            .subject(customerId.toString())
            .claim("email", email)
            .claim("roles", roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiration)))
            .signWith(getSigningKey(secret))
            .compact();
    }

    public static String createToken(
        UUID customerId,
        String email,
        String secret,
        Duration expiration) {

    return createToken(
            customerId,
            email,
            secret,
            expiration,
            Set.of("ROLE_CUSTOMER"));
    }

    private static SecretKey getSigningKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
