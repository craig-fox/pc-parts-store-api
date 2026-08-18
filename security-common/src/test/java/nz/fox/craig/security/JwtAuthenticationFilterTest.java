package nz.fox.craig.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;
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

    @Mock private TokenService tokenService;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

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

        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");

        when(tokenService.isTokenValid("jwt-token")).thenReturn(true);

        when(tokenService.extractCustomerId("jwt-token")).thenReturn(CUSTOMER_ID);

        when(tokenService.extractEmail("jwt-token")).thenReturn(EMAIL);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();

        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        assertThat(principal.id()).isEqualTo(CUSTOMER_ID);
        assertThat(principal.email()).isEqualTo(EMAIL);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderMissing() throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearerToken() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldIgnoreInvalidJwt() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");

        when(tokenService.isTokenValid("jwt-token")).thenThrow(new JwtException("Invalid"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldIgnoreJwtWhenCustomerIdCannotBeExtracted() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(tokenService.isTokenValid("jwt-token")).thenReturn(true);
        when(tokenService.extractCustomerId("jwt-token"))
                .thenThrow(new JwtException("Invalid customer id"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldIgnoreJwtWhenEmailCannotBeExtracted() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(tokenService.isTokenValid("jwt-token")).thenReturn(true);
        when(tokenService.extractCustomerId("jwt-token")).thenReturn(CUSTOMER_ID);
        when(tokenService.extractEmail("jwt-token")).thenThrow(new JwtException("Invalid email"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReplaceExistingAuthentication() throws Exception {
        String jwt = "valid-jwt";

        AuthenticatedUser existingUser =
                new AuthenticatedUser(
                        UUID.randomUUID(), "existing@example.com", Set.of(Role.ROLE_CUSTOMER));

        UsernamePasswordAuthenticationToken existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        existingUser, jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        when(tokenService.isTokenValid(jwt)).thenReturn(true);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuthentication);

        verify(tokenService).isTokenValid(jwt);
        verify(tokenService, never()).extractCustomerId(jwt);
        verify(tokenService, never()).extractEmail(jwt);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
        String jwt = "invalid-jwt";

        when(tokenService.isTokenValid(jwt)).thenReturn(false);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(tokenService).isTokenValid(jwt);
        verify(tokenService, never()).extractCustomerId(jwt);
        verify(tokenService, never()).extractEmail(jwt);
        verify(filterChain).doFilter(request, response);
    }
}
