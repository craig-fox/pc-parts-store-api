package nz.fox.craig.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.security.config.JwtProperties;
import nz.fox.craig.security.dto.AuthenticatedUser;
import nz.fox.craig.security.dto.Role;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final JwtProperties jwtProperties;
 
    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.expiration());
    
        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("roles", user.roles().stream()
                        .map(Role::name)
                        .toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public AuthenticatedUser parseUser(String token) {
        Claims claims = parseAndValidate(token);
    
        UUID customerId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
    
        List<String> roleNames = claims.get("roles", List.class);
    
        Set<Role> roles = roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    
        return new AuthenticatedUser(customerId, email, roles);
    }

 

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
