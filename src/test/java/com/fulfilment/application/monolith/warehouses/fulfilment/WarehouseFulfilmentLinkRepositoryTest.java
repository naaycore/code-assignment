package com.fulfilment.application.monolith.warehouses.fulfilment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarehouseFulfilmentLinkRepositoryTest {

  private StubRepository repository;

  @BeforeEach
  void setUp() {
    repository = new StubRepository();
  }

  @Test
  void countDistinctWarehousesForStoreUsesEntityManagerQuery() {
    repository.queryResult = 3L;

    var result = repository.countDistinctWarehousesForStore(7L);

    assertEquals(3L, result);
    assertTrue(repository.lastQuery.contains("count(distinct l.warehouseBusinessUnitCode)"));
  }

  @Test
  void countDistinctWarehousesForStoreAndProductUsesEntityManagerQuery() {
    repository.queryResult = 2L;

    var result = repository.countDistinctWarehousesForStoreAndProduct(7L, 11L);

    assertEquals(2L, result);
    assertTrue(repository.lastQuery.contains("l.productId = :productId"));
  }

  @Test
  void countDistinctProductsForWarehouseUsesEntityManagerQuery() {
    repository.queryResult = 5L;

    var result = repository.countDistinctProductsForWarehouse("MWH.1");

    assertEquals(5L, result);
    assertTrue(repository.lastQuery.contains("count(distinct l.productId)"));
  }

  @Test
  void existsMethodsUseCountResults() {
    repository.countByQuery.put("storeId = ?1 and productId = ?2 and warehouseBusinessUnitCode = ?3", 1L);
    repository.countByQuery.put("storeId = ?1 and warehouseBusinessUnitCode = ?2", 0L);
    repository.countByQuery.put("warehouseBusinessUnitCode = ?1 and productId = ?2", 2L);

    assertTrue(repository.existsByStoreAndProductAndWarehouse(1L, 2L, "MWH.1"));
    assertFalse(repository.existsByStoreAndWarehouse(1L, "MWH.1"));
    assertTrue(repository.existsByWarehouseAndProduct("MWH.1", 2L));
  }

  @Test
  void listAllLinksDelegatesToListAll() {
    var link = new WarehouseFulfilmentLink();
    repository.all = List.of(link);

    var result = repository.listAllLinks();

    assertEquals(1, result.size());
    assertSame(link, result.get(0));
  }

  private static class StubRepository extends WarehouseFulfilmentLinkRepository {
    private final Map<String, Long> countByQuery = new HashMap<>();
    private Long queryResult = 0L;
    private String lastQuery;
    private List<WarehouseFulfilmentLink> all = List.of();

    @Override
    public EntityManager getEntityManager() {
      TypedQuery<Long> typedQuery =
          (TypedQuery<Long>)
              Proxy.newProxyInstance(
                  TypedQuery.class.getClassLoader(),
                  new Class<?>[] {TypedQuery.class},
                  (proxy, method, args) -> {
                    if ("setParameter".equals(method.getName())) {
                      return proxy;
                    }
                    if ("getSingleResult".equals(method.getName())) {
                      return queryResult;
                    }
                    return null;
                  });

      return (EntityManager)
          Proxy.newProxyInstance(
              EntityManager.class.getClassLoader(),
              new Class<?>[] {EntityManager.class},
              (proxy, method, args) -> {
                if ("createQuery".equals(method.getName())) {
                  lastQuery = (String) args[0];
                  return typedQuery;
                }
                return null;
              });
    }

    @Override
    public long count(String query, Object... params) {
      return countByQuery.getOrDefault(query, 0L);
    }

    @Override
    public List<WarehouseFulfilmentLink> listAll() {
      return all;
    }
  }
}
