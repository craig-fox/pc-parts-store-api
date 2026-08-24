package nz.fox.craig.order.client;

import java.util.UUID;

import nz.fox.craig.order.dto.request.InventoryReservationRequest;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import nz.fox.craig.order.exception.InsufficientStockException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class HttpInventoryClient implements InventoryClient {

    private final RestClient restClient;

    public HttpInventoryClient(
            @Qualifier("inventoryRestClient") RestClient restClient) {

        this.restClient = restClient;
    }

    @Override
    public void reserveStock(UUID productId, int quantity) {

        try {
            restClient
                    .post()
                    .uri("/api/inventory/{productId}/reserve", productId)
                    .body(new InventoryReservationRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Conflict ex) {
            throw new InsufficientStockException(productId);

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Inventory",
                    ex);
        }
    }

    @Override
    public void releaseStock(UUID productId, int quantity) {

        try {
            restClient
                    .post()
                    .uri("/api/inventory/{productId}/release", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InventoryReservationRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Inventory",
                    ex);
        }
    }
}