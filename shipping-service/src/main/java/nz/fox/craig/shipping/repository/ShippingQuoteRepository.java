package nz.fox.craig.shipping.repository;

import nz.fox.craig.shipping.model.ShippingQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShippingQuoteRepository extends JpaRepository<ShippingQuote, UUID> {

    List<ShippingQuote> findByOrderId(UUID orderId);
}
