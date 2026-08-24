package nz.fox.craig.order.client;

import nz.fox.craig.order.dto.request.ShippingQuoteRequest;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;

public interface ShippingClient {
     ShippingQuoteResponse calculateQuote(ShippingQuoteRequest request);
}
