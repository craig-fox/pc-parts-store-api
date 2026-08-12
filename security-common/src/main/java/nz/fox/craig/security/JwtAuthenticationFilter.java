package nz.fox.craig.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("=== JWT FILTER ===");
        System.out.println("METHOD: " + request.getMethod());
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("AUTH HEADER: " + request.getHeader("Authorization"));

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("NO BEARER TOKEN");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);

            System.out.println("TOKEN VALID: " + tokenService.isTokenValid(jwt));

            if (tokenService.isTokenValid(jwt)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UUID customerId = tokenService.extractCustomerId(jwt);

                AuthenticatedUser principal =
                        new AuthenticatedUser(
                                customerId,
                                tokenService.extractEmail(jwt),
                                Set.of(Role.ROLE_CUSTOMER));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                jwt,
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                System.out.println("AUTHENTICATED CUSTOMER: " + customerId);
            }

        } catch (JwtException | IllegalArgumentException ex) {
            System.out.println("JWT ERROR: " + ex.getMessage());
        }

        System.out.println(
                "SECURITY CONTEXT: " +
                SecurityContextHolder.getContext().getAuthentication());

        filterChain.doFilter(request, response);
    }
      
}
