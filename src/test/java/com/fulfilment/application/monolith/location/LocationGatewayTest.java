package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  void resolveByIdentifierReturnsLocationWhenFound() {
    var locationGateway = new LocationGateway();

    var location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  void resolveByIdentifierReturnsNullWhenNotFound() {
    var locationGateway = new LocationGateway();

    var location = locationGateway.resolveByIdentifier("UNKNOWN-999");

    assertNull(location);
  }
}
