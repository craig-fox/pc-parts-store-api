package nz.fox.craig.shipping.service;

import nz.fox.craig.shipping.exception.ShippingQuoteExpiredException;
import nz.fox.craig.shipping.exception.ShippingQuoteNotFoundException;
import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.shipping.dto.ShippingAddressRequest;
import nz.fox.craig.shipping.exception.ShipmentNotFoundException;
import nz.fox.craig.shipping.fixture.ShippingFixture;
import nz.fox.craig.shipping.mapper.ShippingMapper;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.repository.ShippingQuoteRepository;
import nz.fox.craig.shipping.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippingQuoteRepository shippingQuoteRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShippingRateCalculator shippingRateCalculator;

    @Mock ShippingMapper shippingMapper;

    @InjectMocks
    private ShippingService shippingService;

    @Test
    void shouldCalculateAndSaveStandardShippingQuote() {
        UUID orderId = UUID.randomUUID();
       
        when(shippingQuoteRepository.save(any(ShippingQuote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        BigDecimal weight = new BigDecimal("2.500");

        when(shippingRateCalculator.calculate(
                weight,
                ShippingMethod.STANDARD))
                .thenReturn(new ShippingRate(
                        new BigDecimal("25.00"),
                        2,
                        5
                ));

        when(shippingMapper.fromAddressDto(any(ShippingAddressRequest.class))).thenCallRealMethod();
        ShippingQuote quote = shippingService.calculateQuote(
                orderId,
                ShippingFixture.shippingAddressRequest(),
                weight,
                ShippingMethod.STANDARD
        );

        assertThat(quote.getOrderId()).isEqualTo(orderId);
        assertThat(quote.getDestination()).usingRecursiveComparison().isEqualTo(ShippingFixture.shippingAddress());
        assertThat(quote.getWeightKg()).isEqualByComparingTo("2.500");
        assertThat(quote.getShippingMethod()).isEqualTo(ShippingMethod.STANDARD);
        assertThat(quote.getPrice()).isEqualByComparingTo("25.00");
        assertThat(quote.getCurrency()).isEqualTo("NZD");
        assertThat(quote.getEstimatedDeliveryMin()).isEqualTo(2);
        assertThat(quote.getEstimatedDeliveryMax()).isEqualTo(5);
        assertThat(quote.getExpiresAt()).isAfter(quote.getCreatedAt());

        verify(shippingQuoteRepository).save(any(ShippingQuote.class));
        verify(shippingRateCalculator).calculate(
                new BigDecimal("2.500"),
                ShippingMethod.STANDARD
        );
    }

    @Test
    void shouldCalculateAndSaveExpressShippingQuote() {
        UUID orderId = UUID.randomUUID();

        when(shippingQuoteRepository.save(any(ShippingQuote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(shippingRateCalculator.calculate(
                new BigDecimal("2.500"),
                ShippingMethod.EXPRESS))
                .thenReturn(new ShippingRate(
                        new BigDecimal("40.00"),
                        1,
                        2
                ));

        ShippingQuote quote = shippingService.calculateQuote(
                orderId,
                ShippingFixture.shippingAddressRequest(),
                new BigDecimal("2.500"),
                ShippingMethod.EXPRESS
        );

        assertThat(quote.getShippingMethod()).isEqualTo(ShippingMethod.EXPRESS);
        assertThat(quote.getPrice()).isEqualByComparingTo("40.00");
        assertThat(quote.getEstimatedDeliveryMin()).isEqualTo(1);
        assertThat(quote.getEstimatedDeliveryMax()).isEqualTo(2);

        verify(shippingQuoteRepository).save(any(ShippingQuote.class));
        verify(shippingRateCalculator).calculate(
                new BigDecimal("2.500"),
                ShippingMethod.EXPRESS
        );
    }

    @Test
    void shouldGetShippingQuote() {
        UUID orderId = UUID.randomUUID();
        ShippingQuote quote = ShippingFixture.shippingQuote(orderId);

        when(shippingQuoteRepository.findById(orderId))
                .thenReturn(java.util.Optional.of(quote));

        ShippingQuote result = shippingService.getQuote(orderId);

        assertThat(result).isSameAs(quote);
        verify(shippingQuoteRepository).findById(orderId);
    }

    @Test
    void shouldThrowWhenShippingQuoteDoesNotExist() {
        UUID orderId = UUID.randomUUID();

        when(shippingQuoteRepository.findById(orderId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> shippingService.getQuote(orderId))
                .isInstanceOf(ShippingQuoteNotFoundException.class)
                .hasMessage("Shipping quote not found: " + orderId);
    }

    @Test
    void shouldGetQuotesForOrder() {
        UUID orderId = UUID.randomUUID();
        List<ShippingQuote> quotes = List.of(
                ShippingFixture.shippingQuote(orderId),
                ShippingFixture.shippingQuote(orderId)
        );

        when(shippingQuoteRepository.findByOrderId(orderId))
                .thenReturn(quotes);

        List<ShippingQuote> result =
                shippingService.getQuotesForOrder(orderId);

        assertThat(result).containsExactlyElementsOf(quotes);
        verify(shippingQuoteRepository).findByOrderId(orderId);
    }

    @Test
    void shouldCreateShipmentFromValidQuote() {
        UUID quoteId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        ShippingQuote quote = ShippingQuote.builder()
                .id(quoteId)
                .orderId(orderId)
                .destination(ShippingFixture.shippingAddress())
                .weightKg(new BigDecimal("2.500"))
                .shippingMethod(ShippingMethod.EXPRESS)
                .price(new BigDecimal("40.00"))
                .currency("NZD")
                .estimatedDeliveryMin(1)
                .estimatedDeliveryMax(2)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();

        when(shippingQuoteRepository.findById(quoteId))
                .thenReturn(java.util.Optional.of(quote));

        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Shipment shipment =
                shippingService.createShipment(quoteId);

        assertThat(shipment.getOrderId()).isEqualTo(orderId);
        assertThat(shipment.getDestination())
                .isEqualTo(quote.getDestination());
        assertThat(shipment.getWeightKg())
                .isEqualByComparingTo(quote.getWeightKg());
        assertThat(shipment.getShippingMethod())
                .isEqualTo(quote.getShippingMethod());
        assertThat(shipment.getShippingCost())
                .isEqualByComparingTo(quote.getPrice());
        assertThat(shipment.getCurrency())
                .isEqualTo(quote.getCurrency());

        verify(shippingQuoteRepository).findById(quoteId);
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void shouldRejectExpiredShippingQuote() {
        UUID quoteId = UUID.randomUUID();

        ShippingQuote quote = ShippingQuote.builder()
                .id(quoteId)
                .orderId(UUID.randomUUID())
                .destination(ShippingFixture.shippingAddress())
                .weightKg(new BigDecimal("2.500"))
                .shippingMethod(ShippingMethod.STANDARD)
                .price(new BigDecimal("25.00"))
                .currency("NZD")
                .estimatedDeliveryMin(2)
                .estimatedDeliveryMax(5)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(shippingQuoteRepository.findById(quoteId))
                .thenReturn(java.util.Optional.of(quote));

        assertThatThrownBy(() -> shippingService.createShipment(quoteId))
                .isInstanceOf(ShippingQuoteExpiredException.class)
                .hasMessage("Shipping quote has expired: " + quoteId);

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void shouldGetShipment() {
        UUID shipmentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Shipment shipment = ShippingFixture.shipment(shipmentId, orderId);

        when(shipmentRepository.findById(shipmentId))
                .thenReturn(java.util.Optional.of(shipment));

        Shipment result = shippingService.getShipment(shipmentId);

        assertThat(result).isSameAs(shipment);
        verify(shipmentRepository).findById(shipmentId);
    }

    @Test
    void shouldThrowWhenShipmentDoesNotExist() {
        UUID shipmentId = UUID.randomUUID();

        when(shipmentRepository.findById(shipmentId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> shippingService.getShipment(shipmentId))
                .isInstanceOf(ShipmentNotFoundException.class)
                .hasMessage("Shipment not found: " + shipmentId);
    }

    @Test
    void shouldGetShipmentsForOrder() {
        UUID orderId = UUID.randomUUID();
        List<Shipment> shipments = List.of(
                ShippingFixture.shipment(orderId),
                ShippingFixture.shipment(orderId)
        );

        when(shipmentRepository.findByOrderId(orderId))
                .thenReturn(shipments);

        List<Shipment> result =
                shippingService.getShipmentsForOrder(orderId);

        assertThat(result).containsExactlyElementsOf(shipments);
        verify(shipmentRepository).findByOrderId(orderId);
    }
}
