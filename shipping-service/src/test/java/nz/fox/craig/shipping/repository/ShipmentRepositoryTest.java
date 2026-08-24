package nz.fox.craig.shipping.repository;

import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.shipping.fixture.ShippingFixture;
import nz.fox.craig.shipping.model.Shipment;
import nz.fox.craig.shipping.model.ShipmentStatus;
import nz.fox.craig.shipping.model.ShippingAddress;
import nz.fox.craig.test.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ShipmentRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    void shouldSaveAndRetrieveShipment() {
        UUID orderId = UUID.randomUUID();

        Shipment shipment = ShippingFixture.shipment(orderId);
        Shipment savedShipment = shipmentRepository.save(shipment);
        Shipment retrievedShipment = shipmentRepository
                .findById(savedShipment.getId())
                .orElseThrow();

        assertThat(retrievedShipment.getId()).isEqualTo(savedShipment.getId());
        assertThat(retrievedShipment.getOrderId()).isEqualTo(orderId);

        assertThat(retrievedShipment.getDestination().getAddressLine1())
                .isEqualTo("123 Test Street");
        assertThat(retrievedShipment.getDestination().getCity())
                .isEqualTo("Auckland");
        assertThat(retrievedShipment.getDestination().getPostcode())
                .isEqualTo("1010");
        assertThat(retrievedShipment.getDestination().getCountry())
                .isEqualTo("NZ");

        assertThat(retrievedShipment.getWeightKg())
                .isEqualByComparingTo("2.500");
        assertThat(retrievedShipment.getShippingMethod())
                .isEqualTo(ShippingMethod.STANDARD);
        assertThat(retrievedShipment.getShippingCost())
                .isEqualByComparingTo("25.00");
        assertThat(retrievedShipment.getCurrency())
                .isEqualTo("NZD");
        assertThat(retrievedShipment.getStatus())
                .isEqualTo(ShipmentStatus.CREATED);
        assertThat(retrievedShipment.getTrackingNumber())
                .isEqualTo("TEST123456");
        assertThat(retrievedShipment.getCreatedAt())
                .isEqualTo(shipment.getCreatedAt());
        assertThat(retrievedShipment.getUpdatedAt())
                .isEqualTo(shipment.getUpdatedAt());
    }

    @Test
    void shouldFindShipmentsByOrderId() {
        UUID orderId = UUID.randomUUID();
        UUID anotherOrderId = UUID.randomUUID();

        Shipment firstShipment = createShipment(
                orderId,
                ShipmentStatus.CREATED,
                "TEST123456"
        );

        Shipment secondShipment = createShipment(
                orderId,
                ShipmentStatus.IN_TRANSIT,
                "TEST789012"
        );

        Shipment otherOrderShipment = createShipment(
                anotherOrderId,
                ShipmentStatus.CREATED,
                "TEST345678"
        );

        shipmentRepository.saveAll(
                List.of(firstShipment, secondShipment, otherOrderShipment)
        );

        List<Shipment> shipments =
                shipmentRepository.findByOrderId(orderId);

        assertThat(shipments)
                .hasSize(2)
                .extracting(Shipment::getOrderId)
                .containsOnly(orderId);
    }

    @Test
    void shouldSaveShipmentWithoutTrackingNumber() {
        Shipment shipment = createShipment(
                UUID.randomUUID(),
                ShipmentStatus.CREATED,
                null
        );

        Shipment savedShipment = shipmentRepository.save(shipment);

        Shipment retrievedShipment = shipmentRepository
                .findById(savedShipment.getId())
                .orElseThrow();

        assertThat(retrievedShipment.getTrackingNumber()).isNull();
    }

    private Shipment createShipment(
            UUID orderId,
            ShipmentStatus status,
            String trackingNumber) {

        LocalDateTime now = LocalDateTime.now();

        return Shipment.builder()
                .orderId(orderId)
                .destination(new ShippingAddress(
                        "123 Test Street",
                        "Auckland",
                        "1010",
                        "NZ"
                ))
                .weightKg(new BigDecimal("1.500"))
                .shippingMethod(ShippingMethod.STANDARD)
                .shippingCost(new BigDecimal("15.00"))
                .currency("NZD")
                .status(status)
                .trackingNumber(trackingNumber)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
