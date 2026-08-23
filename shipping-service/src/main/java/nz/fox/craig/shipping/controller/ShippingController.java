package nz.fox.craig.shipping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.shipping.dto.CreateShipmentRequest;
import nz.fox.craig.shipping.dto.ShipmentResponse;
import nz.fox.craig.shipping.dto.ShippingAddressResponse;
import nz.fox.craig.shipping.dto.ShippingQuoteRequest;
import nz.fox.craig.shipping.dto.ShippingQuoteResponse;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.model.ShippingAddress;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.shipping.service.ShippingService;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/quotes")
    public ResponseEntity<ShippingQuoteResponse> createQuote(
            @Valid @RequestBody ShippingQuoteRequest request) {

        ShippingQuote quote = shippingService.calculateQuote(
                request.orderId(),
                request.destinationRequest(),
                request.weightKg(),
                request.shippingMethod()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(quote));
    }

    @GetMapping("/quotes/{id}")
    public ShippingQuoteResponse getQuote(@PathVariable UUID id) {
        return toResponse(shippingService.getQuote(id));
    }

    @GetMapping("/quotes/order/{orderId}")
    public List<ShippingQuoteResponse> getQuotesForOrder(
            @PathVariable UUID orderId) {

        return shippingService.getQuotesForOrder(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/shipments")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {

        Shipment shipment =
                shippingService.createShipment(request.quoteId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(shipment));
    }

    @GetMapping("/shipments/{id}")
    public ShipmentResponse getShipment(@PathVariable UUID id) {
        return toResponse(shippingService.getShipment(id));
    }

    @GetMapping("/shipments/order/{orderId}")
    public List<ShipmentResponse> getShipmentsForOrder(
            @PathVariable UUID orderId) {

        return shippingService.getShipmentsForOrder(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ShippingQuoteResponse toResponse(ShippingQuote quote) {
        return new ShippingQuoteResponse(
                quote.getId(),
                quote.getOrderId(),
                toResponse(quote.getDestination()),
                quote.getWeightKg(),
                quote.getShippingMethod(),
                quote.getPrice(),
                quote.getCurrency(),
                quote.getEstimatedDeliveryMin(),
                quote.getEstimatedDeliveryMax(),
                quote.getExpiresAt(),
                quote.getCreatedAt()
        );
    }

    private ShippingAddressResponse toResponse(ShippingAddress address) {
        return new ShippingAddressResponse(
                address.getAddressLine1(),
                address.getCity(),
                address.getPostcode(),
                address.getCountry()
        );
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                toResponse(shipment.getDestination()),
                shipment.getWeightKg(),
                shipment.getShippingMethod(),
                shipment.getShippingCost(),
                shipment.getCurrency(),
                shipment.getStatus(),
                shipment.getTrackingNumber(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}
