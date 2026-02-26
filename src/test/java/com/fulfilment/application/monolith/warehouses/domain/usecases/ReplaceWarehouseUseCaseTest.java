package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private InMemoryLocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new InMemoryLocationResolver();
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void replaceThrowsWhenWarehousePayloadIsNull() {
    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(null));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse payload is required.", exception.getMessage());
  }

  @Test
  void replaceThrowsWhenWarehouseDoesNotExist() {
    var replacement = validReplacement();
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 2, 100));

    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));

    assertEquals(404, exception.getResponse().getStatus());
    assertEquals("Warehouse not found.", exception.getMessage());
  }

  @Test
  void replaceThrowsWhenStockDiffersFromCurrentWarehouse() {
    var current = currentWarehouse("MWH.200", "ZWOLLE-001", 10, 5);
    warehouseStore.byBusinessCode.put("MWH.200", current);
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 2, 100));
    var replacement = validReplacement();
    replacement.stock = 3;

    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals(
        "Replacement warehouse stock must match the existing warehouse stock.", exception.getMessage());
  }

  @Test
  void replaceThrowsWhenCapacityCannotAccommodateCurrentStock() {
    var current = currentWarehouse("MWH.200", "ZWOLLE-001", 10, 5);
    warehouseStore.byBusinessCode.put("MWH.200", current);
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 2, 100));
    var replacement = validReplacement();
    replacement.capacity = 4;

    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals(
        "Replacement warehouse capacity must accommodate existing warehouse stock.",
        exception.getMessage());
  }

  @Test
  void replaceThrowsWhenLocationIsInvalid() {
    var current = currentWarehouse("MWH.200", "ZWOLLE-001", 10, 5);
    warehouseStore.byBusinessCode.put("MWH.200", current);
    var replacement = validReplacement();

    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Invalid warehouse location.", exception.getMessage());
  }

  @Test
  void replaceThrowsWhenNewLocationWarehouseCountLimitReached() {
    var current = currentWarehouse("MWH.200", "ZWOLLE-001", 10, 5);
    warehouseStore.byBusinessCode.put("MWH.200", current);
    var otherAtTargetLocation = currentWarehouse("MWH.300", "AMSTERDAM-001", 10, 3);
    warehouseStore.allWarehouses.add(otherAtTargetLocation);
    locationResolver.locationsByIdentifier.put("AMSTERDAM-001", new Location("AMSTERDAM-001", 1, 200));
    var replacement = validReplacement();
    replacement.location = "AMSTERDAM-001";

    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Maximum number of warehouses for this location has been reached.", exception.getMessage());
  }

  @Test
  void replaceThrowsWhenCapacityAtLocationWouldExceedMaximum() {
    var current = currentWarehouse("MWH.200", "ZWOLLE-001", 10, 5);
    warehouseStore.byBusinessCode.put("MWH.200", current);
    warehouseStore.allWarehouses.add(currentWarehouse("MWH.210", "ZWOLLE-001", 80, 2));
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 10, 89));
    var replacement = validReplacement();
    replacement.capacity = 20;

    var exception = assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse capacity exceeds location maximum capacity.", exception.getMessage());
  }

  @Test
  void replaceArchivesCurrentAndCreatesNewWarehouseWhenValid() {
    var current = currentWarehouse("MWH.200", "ZWOLLE-001", 10, 5);
    warehouseStore.byBusinessCode.put("MWH.200", current);
    warehouseStore.allWarehouses.add(current);
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 3, 100));
    var replacement = validReplacement();

    useCase.replace(replacement);

    assertEquals(1, warehouseStore.updatedWarehouses.size());
    assertEquals("MWH.200", warehouseStore.updatedWarehouses.get(0).businessUnitCode);
    assertNotNull(warehouseStore.updatedWarehouses.get(0).archivedAt);

    assertEquals(1, warehouseStore.createdWarehouses.size());
    assertEquals("MWH.200", warehouseStore.createdWarehouses.get(0).businessUnitCode);
    assertEquals("ZWOLLE-001", warehouseStore.createdWarehouses.get(0).location);
    assertNotNull(warehouseStore.createdWarehouses.get(0).createdAt);
  }

  private Warehouse validReplacement() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.200";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 12;
    warehouse.stock = 5;
    return warehouse;
  }

  private Warehouse currentWarehouse(String businessCode, String location, Integer capacity, Integer stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = businessCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static class InMemoryLocationResolver implements LocationResolver {
    private final Map<String, Location> locationsByIdentifier = new HashMap<>();

    @Override
    public Location resolveByIdentifier(String identifier) {
      return locationsByIdentifier.get(identifier);
    }
  }

  private static class InMemoryWarehouseStore implements WarehouseStore {
    private final List<Warehouse> allWarehouses = new ArrayList<>();
    private final List<Warehouse> updatedWarehouses = new ArrayList<>();
    private final List<Warehouse> createdWarehouses = new ArrayList<>();
    private final Map<String, Warehouse> byBusinessCode = new HashMap<>();

    @Override
    public List<Warehouse> getAll() {
      return allWarehouses;
    }

    @Override
    public void create(Warehouse warehouse) {
      createdWarehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {
      updatedWarehouses.add(warehouse);
    }

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return byBusinessCode.get(buCode);
    }
  }
}
