package nz.fox.craig.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nz.fox.craig.inventory.model.Inventory;

import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
}