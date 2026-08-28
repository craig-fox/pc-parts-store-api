package nz.fox.craig.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.slf4j.LoggerFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class RestClientLoggingInterceptorTest {

    private final RestClientLoggingInterceptor interceptor =
            new RestClientLoggingInterceptor();

    private Logger logger;
    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(
                RestClientLoggingInterceptor.class);

        appender = new ListAppender<>();
        appender.start();

        logger.addAppender(appender);
        logger.setAdditive(false);
    }


    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsSuccessfulDownstreamRequest() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response =
                mock(ClientHttpResponse.class);

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(
                java.net.URI.create("http://product-service/api/products/123"));
        when(response.getStatusCode()).thenReturn(
                org.springframework.http.HttpStatus.OK);
        when(execution.execute(request, new byte[0]))
                .thenReturn(response);

        ClientHttpResponse result =
                interceptor.intercept(request, new byte[0], execution);

        assertThat(result).isSameAs(response);

        verify(execution).execute(request, new byte[0]);

        assertThat(appender.list)
                .hasSize(1);

        var event = appender.list.getFirst();

        assertThat(event.getLevel())
                .isEqualTo(Level.INFO);

        assertThat(event.getFormattedMessage())
                .contains("Downstream HTTP request completed")
                .contains("GET")
                .contains("/api/products/123")
                .contains("200")
                .contains("ms");
    }

    @Test
    void logsFailedDownstreamRequestAndRethrowsException()
            throws Exception {

        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);

        IOException exception =
                new IOException("Connection failed");

        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(
                java.net.URI.create("http://payment-service/api/payments"));
        when(execution.execute(request, new byte[0]))
                .thenThrow(exception);

        assertThatThrownBy(() ->
                interceptor.intercept(
                        request,
                        new byte[0],
                        execution))
                .isSameAs(exception);

        verify(execution).execute(request, new byte[0]);

        assertThat(appender.list)
                .hasSize(1);

        var event = appender.list.getFirst();

        assertThat(event.getLevel())
                .isEqualTo(Level.WARN);

        assertThat(event.getFormattedMessage())
                .contains("Downstream HTTP request failed")
                .contains("POST")
                .contains("/api/payments")
                .contains("ms");

        assertThat(event.getThrowableProxy())
                .isNotNull();
    }
}
