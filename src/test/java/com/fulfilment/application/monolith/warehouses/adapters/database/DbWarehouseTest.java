package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DbWarehouseTest {

  @Test
  void toWarehouseMapsAllFields() {
    var db = new DbWarehouse();
    db.businessUnitCode = "MWH.1";
    db.location = "ZWOLLE-001";
    db.capacity = 10;
    db.stock = 4;
    db.createdAt = LocalDateTime.now().minusDays(1);
    db.archivedAt = LocalDateTime.now();

    var warehouse = db.toWarehouse();

    assertEquals("MWH.1", warehouse.businessUnitCode);
    assertEquals("ZWOLLE-001", warehouse.location);
    assertEquals(10, warehouse.capacity);
    assertEquals(4, warehouse.stock);
    assertSame(db.createdAt, warehouse.createdAt);
    assertSame(db.archivedAt, warehouse.archivedAt);
  }
}
