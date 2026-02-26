package com.fulfilment.application.monolith.warehouses.fulfilment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

class AssignWarehouseFulfilmentUseCaseValidationTest {

  @Test
  void assignThrows400WhenStoreIdMissing() {
    var useCase = new AssignWarehouseFulfilmentUseCase(null, null, null);

    var ex = assertThrows(WebApplicationException.class, () -> useCase.assign(null, 1L, "MWH.1"));

    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void assignThrows400WhenProductIdMissing() {
    var useCase = new AssignWarehouseFulfilmentUseCase(null, null, null);

    var ex = assertThrows(WebApplicationException.class, () -> useCase.assign(1L, null, "MWH.1"));

    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void assignThrows400WhenWarehouseCodeBlank() {
    var useCase = new AssignWarehouseFulfilmentUseCase(null, null, null);

    var ex = assertThrows(WebApplicationException.class, () -> useCase.assign(1L, 2L, " "));

    assertEquals(400, ex.getResponse().getStatus());
  }
}
