package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StoreModelTest {

  @Test
  void defaultConstructorCreatesEmptyStore() {
    var store = new Store();

    assertNull(store.name);
    assertEquals(0, store.quantityProductsInStock);
  }

  @Test
  void nameConstructorSetsName() {
    var store = new Store("Main");

    assertEquals("Main", store.name);
  }
}
