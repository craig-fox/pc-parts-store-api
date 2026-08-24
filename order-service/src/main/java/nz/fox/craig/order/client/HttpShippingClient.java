package nz.fox.craig.order.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import nz.fox.craig.order.dto.request.ShippingQuoteRequest;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;
import nz.fox.craig.order.exception.ShippingServiceUnavailableException;

@Component
public class HttpShippingClient implements ShippingClient {
    private final RestClient restClient;

    public HttpShippingClient(@Qualifier("shippingRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ShippingQuoteResponse calculateQuote(
            ShippingQuoteRequest request) {
            try {
                return restClient.post()
                        .uri("/api/shipping/quotes")
                        .body(request)
                        .retrieve()
                        .body(ShippingQuoteResponse.class);
    
            } catch (RestClientException exception) {
                throw new ShippingServiceUnavailableException(exception);
            }
    }
    
}
