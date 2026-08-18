package nz.fox.craig.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtPropagationInterceptorTest {

    private final JwtPropagationInterceptor interceptor = new JwtPropagationInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPropagateJwtAsBearerToken() throws IOException {
        String jwt = "test-jwt";

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser(), jwt);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        byte[] body = "request body".getBytes();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);

        when(execution.execute(request, body)).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + jwt);

        assertThat(result).isSameAs(response);
        verify(execution).execute(request, body);
    }

    @Test
    void shouldNotPropagateWhenCredentialsAreNotJwt() throws IOException {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        SampleAuthenticatedUsers.authenticatedCustomerUser(), new Object());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, body, execution);

        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();

        verify(execution).execute(request, body);
    }

    @Test
    void shouldNotPropagateWhenAuthenticationIsMissing() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, body, execution);

        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();

        verify(execution).execute(request, body);
    }
}
