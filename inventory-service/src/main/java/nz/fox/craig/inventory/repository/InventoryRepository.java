package nz.fox.craig.inventory.repository;

import java.util.UUID;
import nz.fox.craig.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {}
