package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    validateWarehousePayload(newWarehouse);

    Warehouse currentWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (currentWarehouse == null) {
      throw new WebApplicationException("Warehouse not found.", 404);
    }

    if (!newWarehouse.stock.equals(currentWarehouse.stock)) {
      throw new WebApplicationException(
          "Replacement warehouse stock must match the existing warehouse stock.", 400);
    }

    if (newWarehouse.capacity < currentWarehouse.stock) {
      throw new WebApplicationException(
          "Replacement warehouse capacity must accommodate existing warehouse stock.", 400);
    }

    var location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException("Invalid warehouse location.", 400);
    }

    var warehousesInLocation =
        warehouseStore.getAll().stream().filter(w -> newWarehouse.location.equals(w.location)).toList();
    int currentCount = warehousesInLocation.size();
    int adjustedCount =
        newWarehouse.location.equals(currentWarehouse.location) ? currentCount : currentCount + 1;
    if (adjustedCount > location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Maximum number of warehouses for this location has been reached.", 400);
    }

    int capacityAtLocation =
        warehousesInLocation.stream().mapToInt(w -> w.capacity == null ? 0 : w.capacity).sum();
    int adjustedCapacity =
        newWarehouse.location.equals(currentWarehouse.location)
            ? capacityAtLocation - currentWarehouse.capacity + newWarehouse.capacity
            : capacityAtLocation + newWarehouse.capacity;
    if (adjustedCapacity > location.maxCapacity) {
      throw new WebApplicationException("Warehouse capacity exceeds location maximum capacity.", 400);
    }

    if (newWarehouse.stock > newWarehouse.capacity) {
      throw new WebApplicationException("Warehouse stock cannot exceed warehouse capacity.", 400);
    }

    currentWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(currentWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }

  private void validateWarehousePayload(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse payload is required.", 400);
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WebApplicationException("Warehouse business unit code is required.", 400);
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WebApplicationException("Warehouse location is required.", 400);
    }
    if (warehouse.capacity == null || warehouse.capacity < 0) {
      throw new WebApplicationException("Warehouse capacity must be zero or greater.", 400);
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WebApplicationException("Warehouse stock must be zero or greater.", 400);
    }
  }
}
