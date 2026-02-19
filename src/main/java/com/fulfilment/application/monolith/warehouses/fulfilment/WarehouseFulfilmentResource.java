package com.fulfilment.application.monolith.warehouses.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@Path("warehouse-fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class WarehouseFulfilmentResource {

  @Inject AssignWarehouseFulfilmentOperation assignWarehouseFulfilmentOperation;
  @Inject WarehouseFulfilmentLinkRepository warehouseFulfilmentLinkRepository;

  @POST
  @Transactional
  public WarehouseFulfilmentRequest create(WarehouseFulfilmentRequest request) {
    if (request == null) {
      throw new WebApplicationException("Request payload is required.", 400);
    }
    assignWarehouseFulfilmentOperation.assign(
        request.storeId, request.productId, request.warehouseBusinessUnitCode);
    return request;
  }

  @GET
  public List<WarehouseFulfilmentRequest> list() {
    return warehouseFulfilmentLinkRepository.listAllLinks().stream()
        .map(
            link -> {
              var response = new WarehouseFulfilmentRequest();
              response.storeId = link.storeId;
              response.productId = link.productId;
              response.warehouseBusinessUnitCode = link.warehouseBusinessUnitCode;
              return response;
            })
        .toList();
  }

  public static class WarehouseFulfilmentRequest {
    public Long storeId;
    public Long productId;
    public String warehouseBusinessUnitCode;
  }
}
