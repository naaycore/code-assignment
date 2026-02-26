package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private InMemoryLocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new InMemoryLocationResolver();
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void createThrowsWhenWarehouseIsNull() {
    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(null));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse payload is required.", exception.getMessage());
  }

  @Test
  void createThrowsWhenBusinessUnitCodeIsMissing() {
    var warehouse = validWarehouse();
    warehouse.businessUnitCode = " ";

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse business unit code is required.", exception.getMessage());
  }

  @Test
  void createThrowsWhenLocationIsMissing() {
    var warehouse = validWarehouse();
    warehouse.location = "";

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse location is required.", exception.getMessage());
  }

  @Test
  void createThrowsWhenCapacityIsInvalid() {
    var warehouse = validWarehouse();
    warehouse.capacity = -1;

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse capacity must be zero or greater.", exception.getMessage());
  }

  @Test
  void createThrowsWhenStockIsInvalid() {
    var warehouse = validWarehouse();
    warehouse.stock = -1;

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse stock must be zero or greater.", exception.getMessage());
  }

  @Test
  void createThrowsWhenBusinessUnitCodeAlreadyExists() {
    var warehouse = validWarehouse();
    warehouseStore.byBusinessCode.put(warehouse.businessUnitCode, new Warehouse());
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 5, 500));

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse business unit code already exists.", exception.getMessage());
  }

  @Test
  void createThrowsWhenLocationIsUnknown() {
    var warehouse = validWarehouse();

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Invalid warehouse location.", exception.getMessage());
  }

  @Test
  void createThrowsWhenLocationWarehouseCountLimitReached() {
    var warehouse = validWarehouse();
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 1, 500));
    var existing = new Warehouse();
    existing.location = "ZWOLLE-001";
    existing.capacity = 20;
    warehouseStore.allWarehouses.add(existing);

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Maximum number of warehouses for this location has been reached.", exception.getMessage());
  }

  @Test
  void createThrowsWhenLocationCapacityLimitReached() {
    var warehouse = validWarehouse();
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 10, 100));
    var existingOne = new Warehouse();
    existingOne.location = "ZWOLLE-001";
    existingOne.capacity = 60;
    warehouseStore.allWarehouses.add(existingOne);
    var existingTwo = new Warehouse();
    existingTwo.location = "ZWOLLE-001";
    existingTwo.capacity = 35;
    warehouseStore.allWarehouses.add(existingTwo);

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse capacity exceeds location maximum capacity.", exception.getMessage());
  }

  @Test
  void createThrowsWhenStockExceedsCapacity() {
    var warehouse = validWarehouse();
    warehouse.capacity = 9;
    warehouse.stock = 10;
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 10, 200));

    var exception = assertThrows(WebApplicationException.class, () -> useCase.create(warehouse));

    assertEquals(400, exception.getResponse().getStatus());
    assertEquals("Warehouse stock cannot exceed warehouse capacity.", exception.getMessage());
  }

  @Test
  void createPersistsWarehouseWhenPayloadIsValid() {
    var warehouse = validWarehouse();
    warehouse.createdAt = null;
    warehouse.archivedAt = null;
    locationResolver.locationsByIdentifier.put("ZWOLLE-001", new Location("ZWOLLE-001", 2, 200));
    var existing = new Warehouse();
    existing.location = "ZWOLLE-001";
    existing.capacity = null;
    warehouseStore.allWarehouses.add(existing);

    useCase.create(warehouse);

    assertEquals(1, warehouseStore.createdWarehouses.size());
    assertEquals("MWH.100", warehouseStore.createdWarehouses.get(0).businessUnitCode);
    assertNotNull(warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  private Warehouse validWarehouse() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 10;
    warehouse.stock = 5;
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
    private final List<Warehouse> createdWarehouses = new ArrayList<>();
    private final Map<String, Warehouse> byBusinessCode = new HashMap<>();

    @Override
    public List<Warehouse> getAll() {
      return allWarehouses;
    }

    @Override
    public void create(Warehouse warehouse) {
      createdWarehouses.add(warehouse);
      byBusinessCode.put(warehouse.businessUnitCode, warehouse);
      allWarehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {}

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return byBusinessCode.get(buCode);
    }
  }
}
