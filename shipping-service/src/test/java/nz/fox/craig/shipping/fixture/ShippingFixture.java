package nz.fox.craig.shipping.fixture;

import nz.fox.craig.shipping.model.ShippingAddress;
import nz.fox.craig.shipping.model.ShippingMethod;
import nz.fox.craig.shipping.model.ShippingQuote;
import nz.fox.craig.shipping.dto.ShippingAddressRequest;
import nz.fox.craig.shipping.mapper.ShippingMapper;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.model.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ShippingFixture {

    private ShippingFixture() {
    }

    public static ShippingAddress shippingAddress() {
        return new ShippingAddress(
                "123 Test Street",
                "Auckland",
                "1010",
                "NZ"
        );
    }

    public static ShippingAddressRequest toAddressRequest(ShippingAddress address) {
        return new ShippingMapper().toAddressDto(address);
    }

    public static ShippingAddressRequest shippingAddressRequest() {
        return toAddressRequest(shippingAddress());
    }

    public static ShippingQuote shippingQuote(UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        return ShippingQuote.builder()
                .orderId(orderId)
                .destination(shippingAddress())
                .weightKg(new BigDecimal("2.500"))
                .shippingMethod(ShippingMethod.STANDARD)
                .price(new BigDecimal("15.00"))
                .currency("NZD")
                .estimatedDeliveryMin(2)
                .estimatedDeliveryMax(5)
                .expiresAt(now.plusHours(1))
                .createdAt(now)
                .build();
    }

    public static ShippingQuote shippingQuote(UUID id, UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        return ShippingQuote.builder()
                .id(id)
                .orderId(orderId)
                .destination(shippingAddress())
                .weightKg(new BigDecimal("2.500"))
                .shippingMethod(ShippingMethod.STANDARD)
                .price(new BigDecimal("15.00"))
                .currency("NZD")
                .estimatedDeliveryMin(2)
                .estimatedDeliveryMax(5)
                .expiresAt(now.plusHours(1))
                .createdAt(now)
                .build();
    }

    public static ShippingQuote shippingQuote(
        UUID orderId,
        ShippingMethod shippingMethod) {

        LocalDateTime now = LocalDateTime.now();

        return ShippingQuote.builder()
                .orderId(orderId)
                .destination(shippingAddress())
                .weightKg(new BigDecimal("1.500"))
                .shippingMethod(shippingMethod)
                .price(new BigDecimal("15.00"))
                .currency("NZD")
                .estimatedDeliveryMin(2)
                .estimatedDeliveryMax(5)
                .expiresAt(now.plusHours(1))
                .createdAt(now)
                .build();
    }

    public static Shipment shipment(UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        return Shipment.builder()
                .orderId(orderId)
                .destination(shippingAddress())
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

    public static Shipment shipment(UUID id, UUID orderId) {
        LocalDateTime now = LocalDateTime.now();

        return Shipment.builder()
                .id(id)
                .orderId(orderId)
                .destination(shippingAddress())
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
