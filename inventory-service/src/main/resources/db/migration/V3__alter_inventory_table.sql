ALTER TABLE inventory
    ADD CONSTRAINT chk_inventory_reserved_lte_on_hand
    CHECK (quantity_reserved <= quantity_on_hand);