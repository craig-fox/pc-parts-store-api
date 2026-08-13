package nz.fox.craig.order.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record ShippingAddress(
    String addressLine1,
    String city,
    String postcode,
    String country
) {
}
