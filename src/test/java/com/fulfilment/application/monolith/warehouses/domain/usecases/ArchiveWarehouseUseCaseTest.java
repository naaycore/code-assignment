package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchiveWarehouseUseCaseTest {

  @Test
  void archiveThrowsWhenWarehouseIsNull() {
    var store = new InMemoryWarehouseStore();
    var useCase = new ArchiveWarehouseUseCase(store);

    var exception = assertThrows(WebApplicationException.class, () -> useCase.archive(null));

    assertEquals(404, exception.getResponse().getStatus());
    assertEquals("Warehouse not found.", exception.getMessage());
  }

  @Test
  void archiveThrowsWhenBusinessUnitCodeIsBlank() {
    var store = new InMemoryWarehouseStore();
    var useCase = new ArchiveWarehouseUseCase(store);
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "  ";

    var exception = assertThrows(WebApplicationException.class, () -> useCase.archive(warehouse));

    assertEquals(404, exception.getResponse().getStatus());
    assertEquals("Warehouse not found.", exception.getMessage());
  }

  @Test
  void archiveSetsArchivedAtAndDelegatesUpdate() {
    var store = new InMemoryWarehouseStore();
    var useCase = new ArchiveWarehouseUseCase(store);
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertEquals(1, store.updatedWarehouses.size());
    assertEquals("MWH.100", store.updatedWarehouses.get(0).businessUnitCode);
  }

  private static class InMemoryWarehouseStore implements WarehouseStore {
    private final List<Warehouse> updatedWarehouses = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return List.of();
    }

    @Override
    public void create(Warehouse warehouse) {}

    @Override
    public void update(Warehouse warehouse) {
      updatedWarehouses.add(warehouse);
    }

    @Override
    public void remove(Warehouse warehouse) {}

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return null;
    }
  }
}
