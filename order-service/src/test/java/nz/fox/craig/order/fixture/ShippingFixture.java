package nz.fox.craig.order.fixture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.dto.request.ShippingQuoteRequest;
import nz.fox.craig.order.dto.response.ShippingAddressResponse;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;

public final class ShippingFixture {


    public static ShippingQuoteResponse shippingQuoteResponse(
        ShippingMethod shippingMethod,
        BigDecimal price) {
        
        LocalDateTime now = LocalDateTime.now();
        return ShippingQuoteResponse.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .destination(new ShippingAddressResponse("123 Main Street", "Auckland", "1000", "New Zealand"))
            .weightKg(BigDecimal.valueOf(2.5))
            .shippingMethod(shippingMethod)
            .price(price)
            .currency("NZD")
            .estimatedDeliveryMin(2)
            .estimatedDeliveryMax(5)
            .expiresAt(now.plusHours(12))
            .createdAt(now)
            .build();
    }


    public static ShippingAddressResponse shippingAddressResponse() {
        return new ShippingAddressResponse(
                "123 Test Street",
                "Auckland",
                "1010",
                "NZ"
        );
    }

    public static ShippingAddressRequest shippingAddressRequest() {
        return ShippingAddressRequest.builder()
            .addressLine1("123 Main Street")
            .city("Auckland")
            .postcode("1000")
            .country("New Zealand")
            .build();
    }

    public static ShippingQuoteRequest shippingQuoteRequest() {
        return ShippingQuoteRequest.builder()
            .orderId(UUID.randomUUID())
            .destinationRequest(shippingAddressRequest())
            .weightKg(BigDecimal.valueOf(2.5))
            .shippingMethod(ShippingMethod.STANDARD)
            .build();
    }

}
