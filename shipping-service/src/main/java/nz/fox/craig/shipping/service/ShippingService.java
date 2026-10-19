package nz.fox.craig.shipping.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.shipping.dto.ShippingAddressRequest;
import nz.fox.craig.shipping.exception.ShipmentNotFoundException;
import nz.fox.craig.shipping.exception.ShippingQuoteExpiredException;
import nz.fox.craig.shipping.exception.ShippingQuoteNotFoundException;
import nz.fox.craig.shipping.mapper.ShippingMapper;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.model.ShipmentStatus;
import nz.fox.craig.shipping.model.ShippingAddress;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.shipping.repository.ShipmentRepository;
import nz.fox.craig.shipping.repository.ShippingQuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final String CURRENCY = "NZD";

    private final ShippingQuoteRepository shippingQuoteRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingRateCalculator shippingRateCalculator;
    private final ShippingMapper shippingMapper;

    @Transactional
    public ShippingQuote calculateQuote(
            UUID orderId,
            ShippingAddressRequest destinationRequest,
            BigDecimal weightKg,
            ShippingMethod shippingMethod) {

        ShippingRate rate = shippingRateCalculator.calculate(weightKg, shippingMethod);

        LocalDateTime now = LocalDateTime.now();
        ShippingAddress address = shippingMapper.fromAddressDto(destinationRequest);

        ShippingQuote quote =
                ShippingQuote.builder()
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

    @Transactional(readOnly = true)
    public ShippingQuote getQuote(UUID id) {
        return shippingQuoteRepository
                .findById(id)
                .orElseThrow(() -> new ShippingQuoteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ShippingQuote> getQuotesForOrder(UUID orderId) {
        return shippingQuoteRepository.findByOrderId(orderId);
    }

    @Transactional
    public Shipment createShipment(UUID quoteId) {
        ShippingQuote quote = getQuote(quoteId);
        LocalDateTime now = LocalDateTime.now();

        if (!quote.getExpiresAt().isAfter(now)) {
            throw new ShippingQuoteExpiredException(quoteId);
        }

        Shipment shipment =
                Shipment.builder()
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

    @Transactional(readOnly = true)
    public Shipment getShipment(UUID id) {
        return shipmentRepository.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsForOrder(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }
}
