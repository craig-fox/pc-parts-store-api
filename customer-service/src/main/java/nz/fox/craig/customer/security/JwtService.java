package nz.fox.craig.customer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import nz.fox.craig.customer.model.Customer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(Customer customer) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpiration);

        return Jwts.builder()
                .subject(customer.getId().toString())
                .claim("email", customer.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public boolean isTokenValid(String token, Customer customer) {
        return extractEmail(token).equals(customer.getEmail())
                && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token, CustomerUserDetails userDetails) {

        return extractCustomerId(token).equals(userDetails.getCustomerId())
                && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractEmail(token);
    }

    public UUID extractCustomerId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    private Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

   
}