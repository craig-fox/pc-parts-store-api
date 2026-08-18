package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import nz.fox.craig.order.dto.request.InventoryReservationRequest;
import nz.fox.craig.order.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class HttpInventoryClientTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final int QUANTITY = 3;

    @Mock private RestClient restClient;

    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock private RestClient.RequestBodySpec requestBodySpec;

    @Mock private RestClient.ResponseSpec responseSpec;

    private HttpInventoryClient client;

    @BeforeEach
    void setUp() {
        client = new HttpInventoryClient(restClient);
    }

    @Test
    void shouldReleaseStock() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/inventory/{productId}/release"), eq(PRODUCT_ID)))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(InventoryReservationRequest.class)))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        client.releaseStock(PRODUCT_ID, QUANTITY);

        verify(restClient).post();
        verify(requestBodyUriSpec).uri("/api/inventory/{productId}/release", PRODUCT_ID);
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec).body(new InventoryReservationRequest(QUANTITY));
        verify(requestBodySpec).retrieve();
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void shouldTranslateConflictWhenReservingStock() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/api/inventory/{productId}/reserve"), eq(PRODUCT_ID)))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(InventoryReservationRequest.class)))
                .thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        HttpClientErrorException.Conflict conflict = mock(HttpClientErrorException.Conflict.class);

        when(responseSpec.toBodilessEntity()).thenThrow(conflict);

        assertThatThrownBy(() -> client.reserveStock(PRODUCT_ID, QUANTITY))
                .isInstanceOf(InsufficientStockException.class);
    }
}
