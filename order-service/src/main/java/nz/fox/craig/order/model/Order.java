package nz.fox.craig.order.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "orders",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_orders_customer_idempotency_key",
            columnNames = {"customer_id", "idempotency_key"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "idempotency_request_hash", nullable = false, length = 64)
    private String idempotencyRequestHash;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shipping;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Embedded private ShippingAddress shippingAddress;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
