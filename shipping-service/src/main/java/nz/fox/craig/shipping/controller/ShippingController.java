package nz.fox.craig.shipping.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Shipping quotes and shipment management")
public class ShippingController {

    private final ShippingService shippingService;

    @Operation(summary = "Create a shipping quote")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Shipping quote created"),
        @ApiResponse(responseCode = "400", description = "Invalid quote request"),
        @ApiResponse(responseCode = "404", description = "Order or related resource not found")
    })
    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public ShippingQuoteResponse createQuote(@Valid @RequestBody ShippingQuoteRequest request) {

        ShippingQuote quote =
                shippingService.calculateQuote(
                        request.orderId(),
                        request.destinationRequest(),
                        request.weightKg(),
                        request.shippingMethod());

        return toResponse(quote);
    }

    @Operation(summary = "Get a shipping quote")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipping quote retrieved"),
        @ApiResponse(responseCode = "404", description = "Shipping quote not found")
    })
    @GetMapping("/quotes/{id}")
    public ShippingQuoteResponse getQuote(@PathVariable UUID id) {
        return toResponse(shippingService.getQuote(id));
    }

    @Operation(summary = "Get shipping quotes for an order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipping quotes retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid order ID")
    })
    @GetMapping("/quotes/order/{orderId}")
    public List<ShippingQuoteResponse> getQuotesForOrder(@PathVariable UUID orderId) {

        return shippingService.getQuotesForOrder(orderId).stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Create a shipment from a shipping quote")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Shipment created"),
        @ApiResponse(responseCode = "400", description = "Invalid shipment request"),
        @ApiResponse(responseCode = "404", description = "Shipping quote not found"),
        @ApiResponse(
                responseCode = "409",
                description = "Shipment cannot be created from this quote")
    })
    @PostMapping("/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@Valid @RequestBody CreateShipmentRequest request) {

        Shipment shipment = shippingService.createShipment(request.quoteId());

        return toResponse(shipment);
    }

    @Operation(summary = "Get a shipment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipment retrieved"),
        @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @GetMapping("/shipments/{id}")
    public ShipmentResponse getShipment(@PathVariable UUID id) {
        return toResponse(shippingService.getShipment(id));
    }

    @Operation(summary = "Get shipments for an order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Shipments retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid order ID")
    })
    @GetMapping("/shipments/order/{orderId}")
    public List<ShipmentResponse> getShipmentsForOrder(@PathVariable UUID orderId) {

        return shippingService.getShipmentsForOrder(orderId).stream()
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
                quote.getCreatedAt());
    }

    private ShippingAddressResponse toResponse(ShippingAddress address) {
        return new ShippingAddressResponse(
                address.getAddressLine1(),
                address.getCity(),
                address.getPostcode(),
                address.getCountry());
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
                shipment.getUpdatedAt());
    }
}
