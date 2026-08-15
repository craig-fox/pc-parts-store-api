package nz.fox.craig.order.client;

import java.util.UUID;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class HttpCustomerClient implements CustomerClient {

    private final RestClient restClient;

    public HttpCustomerClient(
            @Qualifier("customerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void validateCustomerExists(UUID customerId) {
        try {
            restClient.head().uri("/api/customers/{id}", customerId).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new CustomerNotFoundException(customerId);
        }
    }
}
