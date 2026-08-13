package nz.fox.craig.order.utils;

import java.util.UUID;

public final class ProductResponses {

    public static String gamingMouse(UUID id) {
        return """
            {
              "id":"%s",
              "name":"Gaming Mouse",
              "price":89.99,
              "weightKg":0.3,
              "active":true
            }
            """
                .formatted(id);
    }
}
