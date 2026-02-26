package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class LegacyStoreManagerGatewayTest {

  @Test
  void createStoreOnLegacySystemDoesNotThrow() {
    var gateway = new LegacyStoreManagerGateway();
    var store = new Store("abc-store");
    store.quantityProductsInStock = 10;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void updateStoreOnLegacySystemDoesNotThrow() {
    var gateway = new LegacyStoreManagerGateway();
    var store = new Store("def-store");
    store.quantityProductsInStock = 20;

    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }
}
