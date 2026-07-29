package nz.fox.craig.inventory.mapper;

import org.mapstruct.Mapper;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.model.Inventory;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    InventoryResponse toResponse(Inventory inventory);
}