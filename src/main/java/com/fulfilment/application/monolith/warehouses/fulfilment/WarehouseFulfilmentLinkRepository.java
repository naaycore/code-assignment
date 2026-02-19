package com.fulfilment.application.monolith.warehouses.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseFulfilmentLinkRepository implements PanacheRepository<WarehouseFulfilmentLink> {

  public long countDistinctWarehousesForStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct l.warehouseBusinessUnitCode) from WarehouseFulfilmentLink l where l.storeId = :storeId",
            Long.class)
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  public long countDistinctWarehousesForStoreAndProduct(Long storeId, Long productId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct l.warehouseBusinessUnitCode) from WarehouseFulfilmentLink l where l.storeId = :storeId and l.productId = :productId",
            Long.class)
        .setParameter("storeId", storeId)
        .setParameter("productId", productId)
        .getSingleResult();
  }

  public long countDistinctProductsForWarehouse(String businessUnitCode) {
    return getEntityManager()
        .createQuery(
            "select count(distinct l.productId) from WarehouseFulfilmentLink l where l.warehouseBusinessUnitCode = :businessUnitCode",
            Long.class)
        .setParameter("businessUnitCode", businessUnitCode)
        .getSingleResult();
  }

  public boolean existsByStoreAndProductAndWarehouse(
      Long storeId, Long productId, String businessUnitCode) {
    return count(
            "storeId = ?1 and productId = ?2 and warehouseBusinessUnitCode = ?3",
            storeId, productId, businessUnitCode)
        > 0;
  }

  public boolean existsByStoreAndWarehouse(Long storeId, String businessUnitCode) {
    return count("storeId = ?1 and warehouseBusinessUnitCode = ?2", storeId, businessUnitCode) > 0;
  }

  public boolean existsByWarehouseAndProduct(String businessUnitCode, Long productId) {
    return count("warehouseBusinessUnitCode = ?1 and productId = ?2", businessUnitCode, productId) > 0;
  }

  public List<WarehouseFulfilmentLink> listAllLinks() {
    return listAll();
  }
}
