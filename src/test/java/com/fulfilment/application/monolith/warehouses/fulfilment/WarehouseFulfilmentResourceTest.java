package com.fulfilment.application.monolith.warehouses.fulfilment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WarehouseFulfilmentResourceTest {

  private WarehouseFulfilmentResource resource;
  private CapturingAssignOperation assignOperation;
  private StubLinkRepository repository;

  @BeforeEach
  void setUp() {
    resource = new WarehouseFulfilmentResource();
    assignOperation = new CapturingAssignOperation();
    repository = new StubLinkRepository();
    resource.assignWarehouseFulfilmentOperation = assignOperation;
    resource.warehouseFulfilmentLinkRepository = repository;
  }

  @Test
  void createThrows400WhenRequestMissing() {
    var ex = assertThrows(WebApplicationException.class, () -> resource.create(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void createDelegatesAndReturnsRequest() {
    var request = new WarehouseFulfilmentResource.WarehouseFulfilmentRequest();
    request.storeId = 1L;
    request.productId = 2L;
    request.warehouseBusinessUnitCode = "MWH.1";

    var response = resource.create(request);

    assertSame(request, response);
    assertEquals(1L, assignOperation.storeId);
    assertEquals(2L, assignOperation.productId);
    assertEquals("MWH.1", assignOperation.warehouseCode);
  }

  @Test
  void listMapsLinksToResponseObjects() {
    var first = new WarehouseFulfilmentLink();
    first.storeId = 1L;
    first.productId = 10L;
    first.warehouseBusinessUnitCode = "MWH.1";

    var second = new WarehouseFulfilmentLink();
    second.storeId = 2L;
    second.productId = 20L;
    second.warehouseBusinessUnitCode = "MWH.2";

    repository.links = List.of(first, second);

    var list = resource.list();

    assertEquals(2, list.size());
    assertEquals(1L, list.get(0).storeId);
    assertEquals(10L, list.get(0).productId);
    assertEquals("MWH.1", list.get(0).warehouseBusinessUnitCode);
    assertEquals(2L, list.get(1).storeId);
  }

  private static class CapturingAssignOperation implements AssignWarehouseFulfilmentOperation {
    private Long storeId;
    private Long productId;
    private String warehouseCode;

    @Override
    public void assign(Long storeId, Long productId, String warehouseBusinessUnitCode) {
      this.storeId = storeId;
      this.productId = productId;
      this.warehouseCode = warehouseBusinessUnitCode;
    }
  }

  private static class StubLinkRepository extends WarehouseFulfilmentLinkRepository {
    private List<WarehouseFulfilmentLink> links = List.of();

    @Override
    public List<WarehouseFulfilmentLink> listAllLinks() {
      return links;
    }
  }
}
