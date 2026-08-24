package nz.fox.craig.shipping.service;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.shipping.exception.ShippingQuoteExpiredException;
import nz.fox.craig.shipping.exception.ShippingQuoteNotFoundException;
import nz.fox.craig.shipping.mapper.ShippingMapper;
import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.shipping.dto.ShippingAddressRequest;
import nz.fox.craig.shipping.exception.ShipmentNotFoundException;
import nz.fox.craig.shipping.model.ShippingAddress;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.model.ShipmentStatus;
import nz.fox.craig.shipping.repository.ShippingQuoteRepository;
import nz.fox.craig.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final String CURRENCY = "NZD";

    private final ShippingQuoteRepository shippingQuoteRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingRateCalculator shippingRateCalculator;
    private final ShippingMapper shippingMapper;

    public ShippingQuote calculateQuote(
            UUID orderId,
            ShippingAddressRequest destinationRequest,
            BigDecimal weightKg,
            ShippingMethod shippingMethod) {

        ShippingRate rate = shippingRateCalculator.calculate(
                weightKg,
                shippingMethod
        );

        LocalDateTime now = LocalDateTime.now();
        ShippingAddress address = shippingMapper.fromAddressDto(destinationRequest);
       

        ShippingQuote quote = ShippingQuote.builder()
                .orderId(orderId)
                .destination(address)
                .weightKg(weightKg)
                .shippingMethod(shippingMethod)
                .price(rate.price())
                .currency(CURRENCY)
                .estimatedDeliveryMin(rate.estimatedDeliveryMin())
                .estimatedDeliveryMax(rate.estimatedDeliveryMax())
                .expiresAt(now.plusHours(1))
                .createdAt(now)
                .build();

        return shippingQuoteRepository.save(quote);
    }

    public ShippingQuote getQuote(UUID id) {
        return shippingQuoteRepository.findById(id)
                .orElseThrow(() -> new ShippingQuoteNotFoundException(id));
    }

    public List<ShippingQuote> getQuotesForOrder(UUID orderId) {
        return shippingQuoteRepository.findByOrderId(orderId);
    }

    public Shipment createShipment(UUID quoteId) {
        ShippingQuote quote = getQuote(quoteId);

        if (!quote.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ShippingQuoteExpiredException(quoteId);
        }

        LocalDateTime now = LocalDateTime.now();

        Shipment shipment = Shipment.builder()
                .orderId(quote.getOrderId())
                .destination(quote.getDestination())
                .weightKg(quote.getWeightKg())
                .shippingMethod(quote.getShippingMethod())
                .shippingCost(quote.getPrice())
                .currency(quote.getCurrency())
                .status(ShipmentStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return shipmentRepository.save(shipment);
    }

    public Shipment getShipment(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    public List<Shipment> getShipmentsForOrder(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

}
