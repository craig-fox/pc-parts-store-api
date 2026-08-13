package nz.fox.craig.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class JwtTestFactory {

    private JwtTestFactory() {}

    public static String createToken(
            UUID customerId, String email, String base64Secret, Duration validity) {

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));

        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity.toMillis());

        return Jwts.builder()
                .subject(customerId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
