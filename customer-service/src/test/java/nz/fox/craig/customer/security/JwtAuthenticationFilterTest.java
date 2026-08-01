package nz.fox.craig.customer.security;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.service.CustomerDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomerDetailsService customerDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CustomerUserDetails userDetails;

    @BeforeEach
    void setUp() {

        Customer customer = Customer.builder()
                .id(CUSTOMER_ID)
                .email("jane@example.com")
                .password("password")
                .status(CustomerStatus.ACTIVE)
                .build();

        userDetails = new CustomerUserDetails(customer);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderMissing() throws Exception {

        when(request.getHeader("Authorization"))
                .thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsInvalid() throws Exception {

        when(request.getHeader("Authorization"))
                .thenReturn("Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    void shouldAuthenticateUserFromJwt() throws Exception {

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer jwt-token");

        when(jwtService.extractCustomerId("jwt-token"))
                .thenReturn(CUSTOMER_ID);

        when(customerDetailsService.loadUserById(CUSTOMER_ID))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid("jwt-token"))
                .thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isEqualTo(userDetails);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldIgnoreInvalidJwt() throws Exception {

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer jwt-token");

        when(jwtService.extractCustomerId("jwt-token"))
                .thenThrow(new JwtException("Invalid"));

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(filterChain).doFilter(request, response);
    }


}
