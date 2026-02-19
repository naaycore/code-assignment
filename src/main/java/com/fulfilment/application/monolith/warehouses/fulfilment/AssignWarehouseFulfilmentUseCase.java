package com.fulfilment.application.monolith.warehouses.fulfilment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class AssignWarehouseFulfilmentUseCase implements AssignWarehouseFulfilmentOperation {

  private final WarehouseFulfilmentLinkRepository warehouseFulfilmentLinkRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductRepository productRepository;

  public AssignWarehouseFulfilmentUseCase(
      WarehouseFulfilmentLinkRepository warehouseFulfilmentLinkRepository,
      WarehouseRepository warehouseRepository,
      ProductRepository productRepository) {
    this.warehouseFulfilmentLinkRepository = warehouseFulfilmentLinkRepository;
    this.warehouseRepository = warehouseRepository;
    this.productRepository = productRepository;
  }

  @Override
  public void assign(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    if (storeId == null || productId == null || warehouseBusinessUnitCode == null
        || warehouseBusinessUnitCode.isBlank()) {
      throw new WebApplicationException("storeId, productId and warehouseBusinessUnitCode are required.", 400);
    }

    if (Store.findById(storeId) == null) {
      throw new WebApplicationException("Store not found.", 404);
    }
    if (productRepository.findById(productId) == null) {
      throw new WebApplicationException("Product not found.", 404);
    }
    if (warehouseRepository.findByBusinessUnitCode(warehouseBusinessUnitCode) == null) {
      throw new WebApplicationException("Warehouse not found.", 404);
    }

    if (warehouseFulfilmentLinkRepository.existsByStoreAndProductAndWarehouse(
        storeId, productId, warehouseBusinessUnitCode)) {
      return;
    }

    long warehousesPerStoreAndProduct =
        warehouseFulfilmentLinkRepository.countDistinctWarehousesForStoreAndProduct(storeId, productId);
    if (warehousesPerStoreAndProduct >= 2) {
      throw new WebApplicationException(
          "A product can be fulfilled by a maximum of 2 warehouses per store.", 400);
    }

    boolean warehouseAlreadyUsedByStore =
        warehouseFulfilmentLinkRepository.existsByStoreAndWarehouse(storeId, warehouseBusinessUnitCode);
    if (!warehouseAlreadyUsedByStore
        && warehouseFulfilmentLinkRepository.countDistinctWarehousesForStore(storeId) >= 3) {
      throw new WebApplicationException(
          "A store can be fulfilled by a maximum of 3 different warehouses.", 400);
    }

    boolean productAlreadyStoredByWarehouse =
        warehouseFulfilmentLinkRepository.existsByWarehouseAndProduct(warehouseBusinessUnitCode, productId);
    if (!productAlreadyStoredByWarehouse
        && warehouseFulfilmentLinkRepository.countDistinctProductsForWarehouse(warehouseBusinessUnitCode)
            >= 5) {
      throw new WebApplicationException(
          "A warehouse can store a maximum of 5 different product types.", 400);
    }

    var link = new WarehouseFulfilmentLink();
    link.storeId = storeId;
    link.productId = productId;
    link.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    warehouseFulfilmentLinkRepository.persist(link);
  }
}
