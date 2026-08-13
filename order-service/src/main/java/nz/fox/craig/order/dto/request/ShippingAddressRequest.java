package nz.fox.craig.order.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ShippingAddressRequest(

    @NotBlank
    String addressLine1,

    @NotBlank
    String city,

    @NotBlank
    String postcode,

    @NotBlank
    String country

) {
}
