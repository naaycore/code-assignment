package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.find("archivedAt is null").list().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    var dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    dbWarehouse.archivedAt = warehouse.archivedAt;
    persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      dbWarehouse =
          find("businessUnitCode = ?1 order by createdAt desc", warehouse.businessUnitCode)
              .firstResult();
    }
    if (dbWarehouse == null) {
      return;
    }

    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse warehouse = find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    return warehouse == null ? null : warehouse.toWarehouse();
  }

  public Warehouse findActiveByIdOrBusinessUnitCode(String idOrCode) {
    DbWarehouse warehouse = find("businessUnitCode = ?1 and archivedAt is null", idOrCode).firstResult();
    if (warehouse != null) {
      return warehouse.toWarehouse();
    }

    try {
      Long id = Long.valueOf(idOrCode);
      warehouse = find("id = ?1 and archivedAt is null", id).firstResult();
      return warehouse == null ? null : warehouse.toWarehouse();
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
