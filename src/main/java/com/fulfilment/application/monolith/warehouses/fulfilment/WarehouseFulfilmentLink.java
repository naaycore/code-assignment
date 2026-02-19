package com.fulfilment.application.monolith.warehouses.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse_fulfilment_link")
@Cacheable
public class WarehouseFulfilmentLink extends PanacheEntity {

  public Long storeId;

  public Long productId;

  public String warehouseBusinessUnitCode;
}
