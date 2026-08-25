package nz.fox.craig.order.client;

import java.util.UUID;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.retry.annotation.Retry;

@Component
public class HttpCustomerClient implements CustomerClient {

    private final RestClient restClient;

    public HttpCustomerClient(
            @Qualifier("customerRestClient") RestClient restClient) {

        this.restClient = restClient;
    }

    @Retry(name = "downstreamRead")
    @Override
    public void validateCustomerExists(UUID customerId) {

        try {
            restClient
                    .head()
                    .uri("/api/customers/{id}", customerId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.NotFound ex) {
            throw new CustomerNotFoundException(customerId);

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Customer",
                    ex);
        }
    }
}
