package nz.fox.craig.order.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.order.exception.CustomerNotFoundException;

@Component
@RequiredArgsConstructor
public class HttpCustomerClient implements CustomerClient {

    @Qualifier("customerRestClient")
    private final RestClient restClient;

    public void validateCustomerExists(UUID customerId) {
        //TODO: Uncomment when customer-service is ready
        // try {
        //     restClient.get()
        //             .uri("/api/customers/{id}", customerId)
        //             .retrieve()
        //             .toBodilessEntity();
        // } catch (HttpClientErrorException.NotFound e) {
        //     throw new CustomerNotFoundException(customerId);
        // }
    }


}
