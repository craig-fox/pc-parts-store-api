package nz.fox.craig.order.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.order.dto.request.InventoryReservationRequest;

@Component
@RequiredArgsConstructor
public class HttpInventoryClient implements InventoryClient {

    @Qualifier("inventoryRestClient")
    private final RestClient restClient;

    public HttpInventoryClient(RestClient.Builder builder,
            @Value("${inventory-service.url}") String inventoryServiceUrl) {
        this.restClient = builder
                .baseUrl(inventoryServiceUrl)
                .build();
    }

    @Override
    public void reserveStock(UUID productId, int quantity) {

        InventoryReservationRequest request = new InventoryReservationRequest(quantity);

        restClient.post()
                .uri("/api/inventory/{productId}/reserve", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void releaseStock(UUID productId, int quantity) {
        InventoryReservationRequest request = new InventoryReservationRequest(quantity);
        restClient.post()
            .uri("/api/inventory/{productId}/release", productId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();
    }

}
