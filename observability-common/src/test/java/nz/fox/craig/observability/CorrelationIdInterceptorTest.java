package nz.fox.craig.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class CorrelationIdInterceptorTest {

    private final CorrelationIdInterceptor interceptor =
            new CorrelationIdInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void addsCorrelationIdToRequestWhenPresentInMdc() throws IOException {
        String correlationId = "test-correlation-123";
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        HttpHeaders headers = new HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(CorrelationId.HEADER))
                .isEqualTo(correlationId);

        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void doesNotAddCorrelationIdWhenNotPresentInMdc() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        HttpHeaders headers = new HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(CorrelationId.HEADER))
                .isNull();

        verify(execution).execute(request, new byte[0]);
    }
}
