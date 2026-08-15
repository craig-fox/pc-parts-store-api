package nz.fox.craig.order.client;

import java.util.UUID;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.exception.ProductNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class HttpProductClient implements ProductClient {

    private final RestClient restClient;

    public HttpProductClient(@Qualifier("productRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ProductSnapshot getProduct(UUID productId) {

        try {
            return restClient
                    .get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductSnapshot.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProductNotFoundException(productId);
        }
    }
}
