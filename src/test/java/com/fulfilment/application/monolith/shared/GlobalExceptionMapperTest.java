package com.fulfilment.application.monolith.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

class GlobalExceptionMapperTest {

  @Test
  void toResponseReturns500ForGenericException() {
    var mapper = new GlobalExceptionMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new RuntimeException("boom"));

    assertEquals(500, response.getStatus());
    assertTrue(response.getEntity().toString().contains("boom"));
  }

  @Test
  void toResponseUsesStatusForWebApplicationException() {
    var mapper = new GlobalExceptionMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new WebApplicationException("bad", 422));

    assertEquals(422, response.getStatus());
    assertTrue(response.getEntity().toString().contains("bad"));
  }
}
