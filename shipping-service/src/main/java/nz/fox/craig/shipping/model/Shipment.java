package nz.fox.craig.shipping.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import nz.fox.craig.api.ShippingMethod;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "shipments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Embedded
    private ShippingAddress destination;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShippingMethod shippingMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(length = 100)
    private String trackingNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}