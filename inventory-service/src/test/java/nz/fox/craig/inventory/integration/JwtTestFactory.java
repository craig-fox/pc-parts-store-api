package nz.fox.craig.inventory.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class JwtTestFactory {

    private JwtTestFactory() {}

    public static String createToken(
            UUID customerId, String email, String secret, Duration expiry) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(customerId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(getSigningKey(secret))
                .compact();
    }

    private static SecretKey getSigningKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
