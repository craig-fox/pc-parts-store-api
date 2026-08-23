package nz.fox.craig.shipping.mapper;

import org.springframework.stereotype.Component;

import nz.fox.craig.shipping.dto.ShippingAddressRequest;
import nz.fox.craig.shipping.model.ShippingAddress;

@Component
public class ShippingMapper {

    public ShippingAddress fromAddressDto(ShippingAddressRequest dto) {
        return new ShippingAddress(dto.addressLine1(), dto.city(), dto.postcode(), dto.country());
    }

    public ShippingAddressRequest toAddressDto(ShippingAddress address) {
        return new ShippingAddressRequest(address.getAddressLine1(), address.getCity(), address.getPostcode(), address.getCountry());
    }

}
