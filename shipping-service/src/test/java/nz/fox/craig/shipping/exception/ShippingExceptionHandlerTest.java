package nz.fox.craig.shipping.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import nz.fox.craig.api.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShippingExceptionHandlerTest {

    private final ShippingExceptionHandler handler =
            new ShippingExceptionHandler();

    private final HttpServletRequest request =
            mock(HttpServletRequest.class);

    @Test
    void shouldHandleConstraintViolationException() {
        when(request.getRequestURI())
                .thenReturn("/api/shipping/quotes");

        ConstraintViolationException exception =
                new ConstraintViolationException("Validation failed", Set.of());

        ResponseEntity<ApiError> response =
                handler.handleConstraintViolationException(
                        exception,
                        request);

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(400);
        assertThat(response.getBody().message())
                .isEqualTo("Validation failed");
        assertThat(response.getBody().path())
                .isEqualTo("/api/shipping/quotes");
    }

    @Test
    void shouldHandleShippingQuoteNotFoundException() {
        UUID quoteId = UUID.randomUUID();
    
        ShippingQuoteNotFoundException exception =
                new ShippingQuoteNotFoundException(quoteId);
    
        when(request.getRequestURI())
                .thenReturn("/api/shipping/quotes/" + quoteId);
    
        ResponseEntity<ApiError> response =
                handler.handleShippingQuoteNotFoundException(
                        exception,
                        request);
    
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().message())
                .isEqualTo(exception.getMessage());
    }

    @Test
    void shouldHandleShipmentNotFoundException() {
        UUID shipmentId = UUID.randomUUID();

        ShipmentNotFoundException exception =
                new ShipmentNotFoundException(shipmentId);

        when(request.getRequestURI())
                .thenReturn("/api/shipping/shipments/" + shipmentId);

        ResponseEntity<ApiError> response =
                handler.handleShipmentNotFoundException(
                        exception,
                        request);

        assertThat(response.getStatusCode().value())
                .isEqualTo(404);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(404);
        assertThat(response.getBody().message())
                .isEqualTo(exception.getMessage());
        assertThat(response.getBody().path())
                .isEqualTo("/api/shipping/shipments/" + shipmentId);
    }

    @Test
    void shouldHandleUnexpectedException() {
        Exception exception =
                new RuntimeException("Unexpected error");

        when(request.getRequestURI())
                .thenReturn("/api/shipping/quotes");

        ResponseEntity<ApiError> response =
                handler.handleException(exception, request);

        assertThat(response.getStatusCode().value())
                .isEqualTo(500);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(500);
        assertThat(response.getBody().message())
                .isEqualTo("Unexpected error");
        assertThat(response.getBody().path())
                .isEqualTo("/api/shipping/quotes");
    }
}
