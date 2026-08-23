package nz.fox.craig.shipping.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressRequest(

    @NotBlank
    String addressLine1,

    @NotBlank
    String city,

    @NotBlank
    String postcode,

    @NotBlank
    String country) {
}
