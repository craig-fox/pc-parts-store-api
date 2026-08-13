package nz.fox.craig.order.client;

import java.util.UUID;
import nz.fox.craig.order.dto.client.ProductSnapshot;

public interface ProductClient {

    ProductSnapshot getProduct(UUID productId);
}
