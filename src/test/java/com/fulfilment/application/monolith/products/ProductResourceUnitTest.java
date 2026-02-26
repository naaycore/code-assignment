package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductResourceUnitTest {

  private ProductResource resource;
  private FakeProductRepository repository;

  @BeforeEach
  void setUp() {
    resource = new ProductResource();
    repository = new FakeProductRepository();
    resource.productRepository = repository;
  }

  @Test
  void getReturnsAllProducts() {
    var a = new Product("A");
    var b = new Product("B");
    repository.list = List.of(a, b);

    var result = resource.get();

    assertEquals(2, result.size());
    assertSame(a, result.get(0));
    assertSame(b, result.get(1));
  }

  @Test
  void getSingleThrows404WhenMissing() {
    var ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(9L));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void getSingleReturnsEntityWhenFound() {
    var product = new Product("Desk");
    repository.byId = product;

    var result = resource.getSingle(5L);

    assertSame(product, result);
  }

  @Test
  void createThrows422WhenIdProvided() {
    var product = new Product("Desk");
    product.id = 1L;

    var ex = assertThrows(WebApplicationException.class, () -> resource.create(product));

    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void createPersistsAndReturns201() {
    var product = new Product("Desk");

    var response = resource.create(product);

    assertEquals(201, response.getStatus());
    assertSame(product, response.getEntity());
    assertEquals(1, repository.persisted.size());
  }

  @Test
  void updateThrows422WhenNameMissing() {
    var product = new Product("Desk");
    product.name = null;

    var ex = assertThrows(WebApplicationException.class, () -> resource.update(1L, product));

    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void updateThrows404WhenTargetMissing() {
    var product = new Product("New");

    var ex = assertThrows(WebApplicationException.class, () -> resource.update(1L, product));

    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void updateMutatesAndPersistsEntity() {
    var entity = new Product("Old");
    repository.byId = entity;

    var update = new Product("New");
    update.description = "d";
    update.price = new BigDecimal("12.34");
    update.stock = 7;

    var result = resource.update(1L, update);

    assertSame(entity, result);
    assertEquals("New", entity.name);
    assertEquals("d", entity.description);
    assertEquals(new BigDecimal("12.34"), entity.price);
    assertEquals(7, entity.stock);
    assertTrue(repository.persisted.contains(entity));
  }

  @Test
  void deleteThrows404WhenMissing() {
    var ex = assertThrows(WebApplicationException.class, () -> resource.delete(4L));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void deleteReturns204WhenFound() {
    var entity = new Product("Desk");
    repository.byId = entity;

    var response = resource.delete(4L);

    assertEquals(204, response.getStatus());
    assertEquals(1, repository.deleted.size());
    assertSame(entity, repository.deleted.get(0));
  }

  @Test
  void errorMapperReturns500ForGenericException() {
    var mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new RuntimeException("boom"));

    assertEquals(500, response.getStatus());
    assertTrue(response.getEntity().toString().contains("boom"));
  }

  @Test
  void errorMapperUsesWebApplicationStatusAndMessage() {
    var mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new WebApplicationException("bad", 422));

    assertEquals(422, response.getStatus());
    assertTrue(response.getEntity().toString().contains("bad"));
  }

  private static class FakeProductRepository extends ProductRepository {
    private List<Product> list = List.of();
    private Product byId;
    private final List<Product> persisted = new ArrayList<>();
    private final List<Product> deleted = new ArrayList<>();

    @Override
    public List<Product> listAll() {
      return list;
    }

    @Override
    public List<Product> listAll(Sort sort) {
      return list;
    }

    @Override
    public Product findById(Long id) {
      return byId;
    }

    @Override
    public void persist(Product entity) {
      persisted.add(entity);
    }

    @Override
    public void delete(Product entity) {
      deleted.add(entity);
    }
  }
}
