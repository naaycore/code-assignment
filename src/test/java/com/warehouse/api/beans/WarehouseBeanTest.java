package com.warehouse.api.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WarehouseBeanTest {

  @Test
  void beanGettersAndSettersWork() {
    var bean = new Warehouse();

    bean.setBusinessUnitCode("MWH.1");
    bean.setLocation("ZWOLLE-001");
    bean.setCapacity(10);
    bean.setStock(5);

    assertEquals("MWH.1", bean.getBusinessUnitCode());
    assertEquals("ZWOLLE-001", bean.getLocation());
    assertEquals(10, bean.getCapacity());
    assertEquals(5, bean.getStock());
  }

  @Test
  void defaultValuesAreNull() {
    var bean = new Warehouse();

    assertNull(bean.getBusinessUnitCode());
    assertNull(bean.getLocation());
    assertNull(bean.getCapacity());
    assertNull(bean.getStock());
  }
}
