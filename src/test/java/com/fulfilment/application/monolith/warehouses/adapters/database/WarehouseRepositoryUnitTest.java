package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarehouseRepositoryUnitTest {

  private StubWarehouseRepository repository;

  @BeforeEach
  void setUp() {
    repository = new StubWarehouseRepository();
  }

  @Test
  void getAllReturnsMappedActiveWarehouses() {
    var db = dbWarehouse("MWH.1", "ZWOLLE-001", 10, 3);
    repository.register("archivedAt is null", List.of(db), db);

    var warehouses = repository.getAll();

    assertEquals(1, warehouses.size());
    assertEquals("MWH.1", warehouses.get(0).businessUnitCode);
  }

  @Test
  void createMapsDomainToDbWarehouseAndPersists() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.2";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 20;
    warehouse.stock = 9;

    repository.create(warehouse);

    assertEquals(1, repository.persisted.size());
    assertEquals("MWH.2", repository.persisted.get(0).businessUnitCode);
    assertEquals("AMSTERDAM-001", repository.persisted.get(0).location);
    assertEquals(20, repository.persisted.get(0).capacity);
    assertEquals(9, repository.persisted.get(0).stock);
  }

  @Test
  void updateReturnsWhenNoMatchingWarehouseExists() {
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(), null);
    repository.register("businessUnitCode = ?1 order by createdAt desc", List.of(), null);

    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "UNKNOWN";
    warehouse.location = "X";
    warehouse.capacity = 1;
    warehouse.stock = 1;

    repository.update(warehouse);

    assertEquals(0, repository.updated.size());
  }

  @Test
  void updateUsesArchivedFallbackWhenActiveNotFound() {
    var fallback = dbWarehouse("MWH.3", "OLD", 10, 2);
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(), null);
    repository.register("businessUnitCode = ?1 order by createdAt desc", List.of(fallback), fallback);

    var replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.3";
    replacement.location = "NEW";
    replacement.capacity = 15;
    replacement.stock = 4;
    replacement.createdAt = LocalDateTime.now();
    replacement.archivedAt = LocalDateTime.now().plusDays(1);

    repository.update(replacement);

    assertEquals("NEW", fallback.location);
    assertEquals(15, fallback.capacity);
    assertEquals(4, fallback.stock);
    assertSame(replacement.createdAt, fallback.createdAt);
    assertSame(replacement.archivedAt, fallback.archivedAt);
  }

  @Test
  void removeDeletesActiveByBusinessCode() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.4";

    repository.remove(warehouse);

    assertEquals("businessUnitCode = ?1 and archivedAt is null", repository.lastDeleteQuery);
    assertEquals("MWH.4", repository.lastDeleteParam);
  }

  @Test
  void findByBusinessUnitCodeReturnsNullWhenMissing() {
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(), null);

    var result = repository.findByBusinessUnitCode("X");

    assertNull(result);
  }

  @Test
  void findByBusinessUnitCodeReturnsMappedWarehouse() {
    var db = dbWarehouse("MWH.5", "TILBURG-001", 8, 3);
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(db), db);

    var result = repository.findByBusinessUnitCode("MWH.5");

    assertEquals("MWH.5", result.businessUnitCode);
    assertEquals("TILBURG-001", result.location);
  }

  @Test
  void findActiveByIdOrBusinessUnitCodeReturnsByCodeFirst() {
    var byCode = dbWarehouse("MWH.6", "ZWOLLE-001", 10, 2);
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(byCode), byCode);

    var result = repository.findActiveByIdOrBusinessUnitCode("MWH.6");

    assertEquals("MWH.6", result.businessUnitCode);
  }

  @Test
  void findActiveByIdOrBusinessUnitCodeReturnsByIdWhenCodeMissing() {
    var byId = dbWarehouse("MWH.7", "AMSTERDAM-001", 12, 4);
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(), null);
    repository.register("id = ?1 and archivedAt is null", List.of(byId), byId);

    var result = repository.findActiveByIdOrBusinessUnitCode("99");

    assertEquals("MWH.7", result.businessUnitCode);
  }

  @Test
  void findActiveByIdOrBusinessUnitCodeReturnsNullWhenCodeNotNumeric() {
    repository.register("businessUnitCode = ?1 and archivedAt is null", List.of(), null);

    var result = repository.findActiveByIdOrBusinessUnitCode("invalid-id");

    assertNull(result);
  }

  private static DbWarehouse dbWarehouse(String code, String location, int capacity, int stock) {
    var db = new DbWarehouse();
    db.businessUnitCode = code;
    db.location = location;
    db.capacity = capacity;
    db.stock = stock;
    return db;
  }

  private static class StubWarehouseRepository extends WarehouseRepository {
    private final Map<String, PanacheQuery<DbWarehouse>> queries = new HashMap<>();
    private final List<DbWarehouse> persisted = new ArrayList<>();
    private final List<DbWarehouse> updated = new ArrayList<>();
    private String lastDeleteQuery;
    private Object lastDeleteParam;

    void register(String query, List<DbWarehouse> list, DbWarehouse first) {
      queries.put(query, panacheQuery(list, first));
    }

    @Override
    public PanacheQuery<DbWarehouse> find(String query, Object... params) {
      return queries.getOrDefault(query, panacheQuery(List.of(), null));
    }

    @Override
    public void persist(DbWarehouse entity) {
      persisted.add(entity);
    }

    @Override
    public long delete(String query, Object... params) {
      lastDeleteQuery = query;
      lastDeleteParam = params.length > 0 ? params[0] : null;
      return 1L;
    }

    private PanacheQuery<DbWarehouse> panacheQuery(List<DbWarehouse> list, DbWarehouse first) {
      return (PanacheQuery<DbWarehouse>)
          Proxy.newProxyInstance(
              PanacheQuery.class.getClassLoader(),
              new Class<?>[] {PanacheQuery.class},
              (proxy, method, args) -> {
                return switch (method.getName()) {
                  case "list" -> list;
                  case "firstResult" -> first;
                  default -> null;
                };
              });
    }
  }
}
