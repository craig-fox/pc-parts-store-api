package nz.fox.craig.inventory.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@EqualsAndHashCode(of = "productId")
@ToString
public class Inventory {

    @Version
    @Column(nullable = false)
    private Long version;

    @Id
    @Column(nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int quantityReserved;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    protected Inventory() {
    }

    public Inventory(UUID productId, int quantityOnHand, int quantityReserved) {
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getAvailableQuantity() {
        return quantityOnHand - quantityReserved;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    
        if (quantity > getAvailableQuantity()) {
            throw new IllegalArgumentException("Insufficient inventory.");
        }
    
        quantityReserved += quantity;
        lastUpdated = LocalDateTime.now();
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    
        if (quantity > quantityReserved) {
            throw new IllegalArgumentException("Cannot release more stock than is reserved.");
        }
    
        quantityReserved -= quantity;
        lastUpdated = LocalDateTime.now();
    }

    public void confirmReservation(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    
        if (quantity > quantityReserved) {
            throw new IllegalArgumentException("Cannot confirm more stock than is reserved.");
        }
    
        quantityReserved -= quantity;
        quantityOnHand -= quantity;
        lastUpdated = LocalDateTime.now();
    }

    public InventoryStatus getStatus() {
        if (quantityOnHand == 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
    
        if (getAvailableQuantity() <= 5) {
            return InventoryStatus.LOW_STOCK;
        }
    
        return InventoryStatus.IN_STOCK;
    }
}
