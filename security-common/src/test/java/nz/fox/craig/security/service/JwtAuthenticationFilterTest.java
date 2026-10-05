package nz.fox.craig.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import nz.fox.craig.security.dto.AuthenticatedUser;
import nz.fox.craig.security.dto.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String EMAIL = "jane@example.com";

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateValidJwt() throws Exception {
        String jwt = "jwt-token";

        AuthenticatedUser user =
                new AuthenticatedUser(
                        CUSTOMER_ID,
                        EMAIL,
                        Set.of(Role.ROLE_CUSTOMER));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(tokenService.parseUser(jwt)).thenReturn(user);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");

        verify(tokenService).parseUser(jwt);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldCreateAuthoritiesFromUserRoles() throws Exception {
        String jwt = "jwt-token";

        AuthenticatedUser user =
                new AuthenticatedUser(
                        CUSTOMER_ID,
                        EMAIL,
                        Set.of(Role.ROLE_CUSTOMER, Role.ROLE_ADMIN));

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(tokenService.parseUser(jwt)).thenReturn(user);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        "ROLE_CUSTOMER",
                        "ROLE_ADMIN");

        verify(tokenService).parseUser(jwt);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderMissing() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(tokenService, never()).parseUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(tokenService, never()).parseUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldIgnoreInvalidJwt() throws Exception {
        String jwt = "invalid-jwt";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(tokenService.parseUser(jwt))
                .thenThrow(new IllegalArgumentException("Invalid JWT"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(tokenService).parseUser(jwt);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReplaceExistingAuthentication() throws Exception {
        String jwt = "valid-jwt";

        AuthenticatedUser existingUser =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        "existing@example.com",
                        Set.of(Role.ROLE_CUSTOMER));

        UsernamePasswordAuthenticationToken existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        existingUser,
                        jwt,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuthentication);

        verify(tokenService, never()).parseUser(jwt);
        verify(filterChain).doFilter(request, response);
    }
}
