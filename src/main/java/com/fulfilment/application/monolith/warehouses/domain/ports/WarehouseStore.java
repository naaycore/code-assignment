package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;
import java.util.Objects;

public interface WarehouseStore {

  List<Warehouse> getAll();

  default List<Warehouse> getByLocation(String location) {
    return getAll().stream().filter(warehouse -> Objects.equals(warehouse.location, location)).toList();
  }

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);
}
