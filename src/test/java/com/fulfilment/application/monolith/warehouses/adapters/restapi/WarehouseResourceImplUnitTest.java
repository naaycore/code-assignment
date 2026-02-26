package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarehouseResourceImplUnitTest {

  private WarehouseResourceImpl resource;
  private FakeWarehouseRepository repository;
  private CapturingCreate createOperation;
  private CapturingReplace replaceOperation;
  private CapturingArchive archiveOperation;

  @BeforeEach
  void setUp() throws Exception {
    resource = new WarehouseResourceImpl();
    repository = new FakeWarehouseRepository();
    createOperation = new CapturingCreate();
    replaceOperation = new CapturingReplace();
    archiveOperation = new CapturingArchive();

    inject(resource, "warehouseRepository", repository);
    inject(resource, "createWarehouseOperation", createOperation);
    inject(resource, "replaceWarehouseOperation", replaceOperation);
    inject(resource, "archiveWarehouseOperation", archiveOperation);
  }

  @Test
  void listAllWarehousesUnitsMapsDomainToResponse() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 10;
    warehouse.stock = 4;
    repository.all.add(warehouse);

    var result = resource.listAllWarehousesUnits();

    assertEquals(1, result.size());
    assertEquals("MWH.100", result.get(0).getBusinessUnitCode());
    assertEquals("ZWOLLE-001", result.get(0).getLocation());
    assertEquals(10, result.get(0).getCapacity());
    assertEquals(4, result.get(0).getStock());
  }

  @Test
  void createANewWarehouseUnitThrows400WhenPayloadIsNull() {
    var ex = assertThrows(WebApplicationException.class, () -> resource.createANewWarehouseUnit(null));

    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void createANewWarehouseUnitDelegatesToUseCaseAndReturnsMappedResponse() {
    var payload = new com.warehouse.api.beans.Warehouse();
    payload.setBusinessUnitCode("MWH.100");
    payload.setLocation("ZWOLLE-001");
    payload.setCapacity(10);
    payload.setStock(3);

    var result = resource.createANewWarehouseUnit(payload);

    assertEquals("MWH.100", result.getBusinessUnitCode());
    assertEquals("ZWOLLE-001", result.getLocation());
    assertEquals(10, result.getCapacity());
    assertEquals(3, result.getStock());

    assertEquals("MWH.100", createOperation.captured.businessUnitCode);
    assertEquals("ZWOLLE-001", createOperation.captured.location);
  }

  @Test
  void getAWarehouseUnitByIDThrows404WhenMissing() {
    var ex = assertThrows(WebApplicationException.class, () -> resource.getAWarehouseUnitByID("x"));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void getAWarehouseUnitByIDReturnsMappedWarehouse() {
    var found = new Warehouse();
    found.businessUnitCode = "MWH.200";
    found.location = "AMSTERDAM-001";
    found.capacity = 20;
    found.stock = 8;
    repository.activeByIdOrCode = found;

    var result = resource.getAWarehouseUnitByID("MWH.200");

    assertEquals("MWH.200", result.getBusinessUnitCode());
    assertEquals("AMSTERDAM-001", result.getLocation());
  }

  @Test
  void archiveAWarehouseUnitByIDThrows404WhenMissing() {
    var ex = assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("x"));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void archiveAWarehouseUnitByIDDelegatesWhenFound() {
    var found = new Warehouse();
    found.businessUnitCode = "MWH.300";
    repository.activeByIdOrCode = found;

    resource.archiveAWarehouseUnitByID("MWH.300");

    assertSame(found, archiveOperation.captured);
  }

  @Test
  void replaceTheCurrentActiveWarehouseThrows500WhenRepositoryReturnsNull() {
    var payload = new com.warehouse.api.beans.Warehouse();
    payload.setLocation("ZWOLLE-001");
    payload.setCapacity(10);
    payload.setStock(5);

    var ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceTheCurrentActiveWarehouse("MWH.500", payload));

    assertEquals(500, ex.getResponse().getStatus());
  }

  @Test
  void replaceTheCurrentActiveWarehouseUsesBusinessCodeAndReturnsSavedWarehouse() {
    var payload = new com.warehouse.api.beans.Warehouse();
    payload.setLocation("ZWOLLE-001");
    payload.setCapacity(10);
    payload.setStock(5);

    var saved = new Warehouse();
    saved.businessUnitCode = "MWH.500";
    saved.location = "ZWOLLE-002";
    saved.capacity = 12;
    saved.stock = 5;
    repository.byBusinessCode = saved;

    var result = resource.replaceTheCurrentActiveWarehouse("MWH.500", payload);

    assertEquals("MWH.500", replaceOperation.captured.businessUnitCode);
    assertEquals("MWH.500", result.getBusinessUnitCode());
    assertEquals("ZWOLLE-002", result.getLocation());
  }

  private static void inject(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static class FakeWarehouseRepository extends WarehouseRepository {
    private final List<Warehouse> all = new ArrayList<>();
    private Warehouse activeByIdOrCode;
    private Warehouse byBusinessCode;

    @Override
    public List<Warehouse> getAll() {
      return all;
    }

    @Override
    public Warehouse findActiveByIdOrBusinessUnitCode(String idOrCode) {
      return activeByIdOrCode;
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return byBusinessCode;
    }
  }

  private static class CapturingCreate implements CreateWarehouseOperation {
    private Warehouse captured;

    @Override
    public void create(Warehouse warehouse) {
      this.captured = warehouse;
    }
  }

  private static class CapturingReplace implements ReplaceWarehouseOperation {
    private Warehouse captured;

    @Override
    public void replace(Warehouse warehouse) {
      this.captured = warehouse;
    }
  }

  private static class CapturingArchive implements ArchiveWarehouseOperation {
    private Warehouse captured;

    @Override
    public void archive(Warehouse warehouse) {
      this.captured = warehouse;
    }
  }
}
