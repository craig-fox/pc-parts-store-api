package nz.fox.craig.order.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.order.dto.response.ShippingAddressResponse;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;

public final class SampleResponses {

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

    public static String shippingQuoteResponseJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
        ShippingQuoteResponse response = new ShippingQuoteResponse(
            UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
            UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7"),
            new ShippingAddressResponse("12 Queen Street", "Auckland", "1010", "NZ"),
            new BigDecimal("2.5"),
            ShippingMethod.STANDARD,
            new BigDecimal("12.99"),
            "NZD",
            3,
            5,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now()
        );
    
        return mapper.writeValueAsString(response);
    }
}
