package nz.fox.craig.order.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import nz.fox.craig.order.dto.request.InventoryReservationRequest;
import nz.fox.craig.order.exception.InsufficientStockException;

@Component
public class HttpInventoryClient implements InventoryClient {

    @Qualifier("inventoryRestClient")
    private final RestClient restClient;

    public HttpInventoryClient(
        @Qualifier("inventoryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void reserveStock(UUID productId, int quantity) {

        try {
            restClient.post()
                    .uri("/api/inventory/{productId}/reserve", productId)
                    .body(new InventoryReservationRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new InsufficientStockException(productId);
        }
    }

    @Override
    public void releaseStock(UUID productId, int quantity) {
        restClient.post()
            .uri("/api/inventory/{productId}/release", productId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new InventoryReservationRequest(quantity))
            .retrieve()
            .toBodilessEntity();
    }

}
