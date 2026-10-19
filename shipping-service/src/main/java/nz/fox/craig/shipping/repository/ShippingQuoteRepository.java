package nz.fox.craig.shipping.repository;

import java.util.List;
import java.util.UUID;
import nz.fox.craig.shipping.model.ShippingQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingQuoteRepository extends JpaRepository<ShippingQuote, UUID> {

    List<ShippingQuote> findByOrderId(UUID orderId);
}
