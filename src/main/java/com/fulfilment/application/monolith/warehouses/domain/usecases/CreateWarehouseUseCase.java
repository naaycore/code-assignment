package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    validateWarehousePayload(warehouse);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException("Warehouse business unit code already exists.", 400);
    }

    var location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException("Invalid warehouse location.", 400);
    }

    var activeWarehousesInLocation =
        warehouseStore.getAll().stream().filter(w -> warehouse.location.equals(w.location)).toList();
    if (activeWarehousesInLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Maximum number of warehouses for this location has been reached.", 400);
    }

    int totalCapacityOnLocation =
        activeWarehousesInLocation.stream().mapToInt(w -> w.capacity == null ? 0 : w.capacity).sum()
            + warehouse.capacity;
    if (totalCapacityOnLocation > location.maxCapacity) {
      throw new WebApplicationException("Warehouse capacity exceeds location maximum capacity.", 400);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException("Warehouse stock cannot exceed warehouse capacity.", 400);
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
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
