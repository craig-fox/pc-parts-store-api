package nz.fox.craig.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestHasher {

    public String hash(OrderRequest request) {

        String canonicalRequest =
                request.items().stream()
                        .map(this::canonicalItem)
                        .collect(Collectors.joining("|"))
                        + "|"
                        + canonicalAddress(request.shippingAddress());

        return sha256(canonicalRequest);
    }

    private String canonicalItem(OrderItemRequest item) {
        return item.productId() + ":" + item.quantity();
    }

    private String canonicalAddress(ShippingAddressRequest address) {
        return String.join(
                "|",
                address.addressLine1(),
                address.city(),
                address.postcode(),
                address.country());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
