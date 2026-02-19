package com.fulfilment.application.monolith.warehouses.fulfilment;

public interface AssignWarehouseFulfilmentOperation {

  void assign(Long storeId, Long productId, String warehouseBusinessUnitCode);
}
