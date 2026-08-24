package nz.fox.craig.order.dto.response;

import lombok.Builder;

@Builder
public record ShippingAddressResponse(
    String addressLine1,
    String city,
    String postcode,
    String country) {
}
