package nz.fox.craig.order.client;

import java.util.UUID;

import nz.fox.craig.order.dto.response.ProductResponse;

public interface ProductClient {

    ProductResponse getProduct(UUID productId);

}
