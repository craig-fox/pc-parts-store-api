package nz.fox.craig.observability;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class RestClientLoggingInterceptor
        implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution)
            throws IOException {

        long start = System.nanoTime();

        try {
            ClientHttpResponse response =
                    execution.execute(request, body);

            long durationMs =
                    (System.nanoTime() - start) / 1_000_000;

            log.info(
                    "Downstream HTTP request completed: {} {} -> {} in {} ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    response.getStatusCode().value(),
                    durationMs);

            return response;

        } catch (java.io.IOException ex) {

            long durationMs =
                    (System.nanoTime() - start) / 1_000_000;

            log.warn(
                    "Downstream HTTP request failed: {} {} after {} ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    durationMs,
                    ex);

            throw ex;
        }
    }
}