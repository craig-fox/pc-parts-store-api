package nz.fox.craig.shipping.dto;

public record ShippingAddressResponse(
        String addressLine1,
        String city,
        String postcode,
        String country) {
}
