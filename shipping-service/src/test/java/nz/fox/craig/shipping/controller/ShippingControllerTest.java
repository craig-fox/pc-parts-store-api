package nz.fox.craig.shipping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nz.fox.craig.shipping.dto.CreateShipmentRequest;
import nz.fox.craig.shipping.dto.ShippingAddressRequest;
import nz.fox.craig.shipping.dto.ShippingQuoteRequest;
import nz.fox.craig.shipping.exception.ShippingExceptionHandler;
import nz.fox.craig.shipping.model.ShippingAddress;
import nz.fox.craig.shipping.model.ShippingMethod;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.model.ShipmentStatus;
import nz.fox.craig.shipping.service.ShippingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShippingController.class)
@Import(ShippingExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShippingService shippingService;

    @Test
    void shouldCreateShippingQuote() throws Exception {
        UUID orderId = UUID.randomUUID();
        ShippingAddressRequest shippingAddressRequest = new ShippingAddressRequest(
                "123 Test Street",
                "Auckland",
                "1010",
                "NZ");

        ShippingQuoteRequest request = new ShippingQuoteRequest(
                orderId,
                shippingAddressRequest,
                new BigDecimal("2.500"),
                ShippingMethod.STANDARD
        );

        ShippingQuote quote = shippingQuote(orderId);

        when(shippingService.calculateQuote(
                eq(orderId),
                any(ShippingAddressRequest.class),
                eq(new BigDecimal("2.500")),
                eq(ShippingMethod.STANDARD)
        )).thenReturn(quote);

        mockMvc.perform(post("/api/shipping/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(quote.getId().toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.destination.addressLine1")
                        .value("123 Test Street"))
                .andExpect(jsonPath("$.destination.city")
                        .value("Auckland"))
                .andExpect(jsonPath("$.destination.postcode")
                        .value("1010"))
                .andExpect(jsonPath("$.destination.country")
                        .value("NZ"))
                .andExpect(jsonPath("$.weightKg").value(2.5))
                .andExpect(jsonPath("$.shippingMethod").value("STANDARD"))
                .andExpect(jsonPath("$.price").value(25.00))
                .andExpect(jsonPath("$.currency").value("NZD"))
                .andExpect(jsonPath("$.estimatedDeliveryMin").value(2))
                .andExpect(jsonPath("$.estimatedDeliveryMax").value(5));

        verify(shippingService).calculateQuote(
                eq(orderId),
                any(ShippingAddressRequest.class),
                eq(new BigDecimal("2.500")),
                eq(ShippingMethod.STANDARD)
        );
    }

    @Test
    void shouldGetShippingQuote() throws Exception {
        UUID quoteId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        ShippingQuote quote = shippingQuote(quoteId, orderId);

        when(shippingService.getQuote(quoteId))
                .thenReturn(quote);

        mockMvc.perform(get("/api/shipping/quotes/{id}", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quoteId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.shippingMethod").value("STANDARD"))
                .andExpect(jsonPath("$.price").value(25.00));

        verify(shippingService).getQuote(quoteId);
    }

    @Test
    void shouldGetQuotesForOrder() throws Exception {
        UUID orderId = UUID.randomUUID();

        ShippingQuote firstQuote =
                shippingQuote(UUID.randomUUID(), orderId);

        ShippingQuote secondQuote =
                shippingQuote(UUID.randomUUID(), orderId);

        when(shippingService.getQuotesForOrder(orderId))
                .thenReturn(List.of(firstQuote, secondQuote));

        mockMvc.perform(
                        get("/api/shipping/quotes/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$[1].orderId")
                        .value(orderId.toString()));

        verify(shippingService).getQuotesForOrder(orderId);
    }

    @Test
    void shouldCreateShipment() throws Exception {
        UUID quoteId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CreateShipmentRequest request =
                new CreateShipmentRequest(quoteId);

        Shipment shipment = shipment(UUID.randomUUID(), orderId);

        when(shippingService.createShipment(quoteId))
                .thenReturn(shipment);

        mockMvc.perform(post("/api/shipping/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(shipment.getId().toString()))
                .andExpect(jsonPath("$.orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$.shippingMethod")
                        .value("STANDARD"))
                .andExpect(jsonPath("$.shippingCost")
                        .value(25.00))
                .andExpect(jsonPath("$.currency")
                        .value("NZD"))
                .andExpect(jsonPath("$.status")
                        .value("CREATED"));

        verify(shippingService).createShipment(quoteId);
    }

    @Test
    void shouldGetShipment() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Shipment shipment = shipment(shipmentId, orderId);

        when(shippingService.getShipment(shipmentId))
                .thenReturn(shipment);

        mockMvc.perform(
                        get("/api/shipping/shipments/{id}", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(shipmentId.toString()))
                .andExpect(jsonPath("$.orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$.shippingMethod")
                        .value("STANDARD"))
                .andExpect(jsonPath("$.shippingCost")
                        .value(25.00))
                .andExpect(jsonPath("$.status")
                        .value("CREATED"));

        verify(shippingService).getShipment(shipmentId);
    }

    @Test
    void shouldGetShipmentsForOrder() throws Exception {
        UUID orderId = UUID.randomUUID();

        Shipment firstShipment =
                shipment(UUID.randomUUID(), orderId);

        Shipment secondShipment =
                shipment(UUID.randomUUID(), orderId);

        when(shippingService.getShipmentsForOrder(orderId))
                .thenReturn(List.of(firstShipment, secondShipment));

        mockMvc.perform(
                        get("/api/shipping/shipments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId")
                        .value(orderId.toString()))
                .andExpect(jsonPath("$[1].orderId")
                        .value(orderId.toString()));

        verify(shippingService).getShipmentsForOrder(orderId);
    }

    @Test
    void shouldRejectInvalidShippingQuoteRequest() throws Exception {
        ShippingQuoteRequest request = new ShippingQuoteRequest(
                null,
                new ShippingAddressRequest(
                        "",
                        "Auckland",
                        "1010",
                        "NZ"
                ),
                BigDecimal.ZERO,
                null
        );

        mockMvc.perform(post("/api/shipping/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(shippingService);
    }

    private ShippingQuote shippingQuote(UUID orderId) {
        return shippingQuote(UUID.randomUUID(), orderId);
    }

    private ShippingQuote shippingQuote(UUID id, UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        return ShippingQuote.builder()
                .id(id)
                .orderId(orderId)
                .destination(new ShippingAddress(
                        "123 Test Street",
                        "Auckland",
                        "1010",
                        "NZ"
                ))
                .weightKg(new BigDecimal("2.500"))
                .shippingMethod(ShippingMethod.STANDARD)
                .price(new BigDecimal("25.00"))
                .currency("NZD")
                .estimatedDeliveryMin(2)
                .estimatedDeliveryMax(5)
                .expiresAt(now.plusHours(1))
                .createdAt(now)
                .build();
    }

    private Shipment shipment(UUID id, UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        return Shipment.builder()
                .id(id)
                .orderId(orderId)
                .destination(new ShippingAddress(
                        "123 Test Street",
                        "Auckland",
                        "1010",
                        "NZ"
                ))
                .weightKg(new BigDecimal("2.500"))
                .shippingMethod(ShippingMethod.STANDARD)
                .shippingCost(new BigDecimal("25.00"))
                .currency("NZD")
                .status(ShipmentStatus.CREATED)
                .trackingNumber("TEST123456")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}