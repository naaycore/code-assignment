package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class StoreResourceSupportTest {

  @Test
  void createThrows422WhenIdIsSet() {
    var resource = new StoreResource();
    var store = new Store("S");
    store.id = 1L;

    var ex = assertThrows(WebApplicationException.class, () -> resource.create(store));

    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void updateThrows422WhenNameMissing() {
    var resource = new StoreResource();
    var store = new Store("S");
    store.name = null;

    var ex = assertThrows(WebApplicationException.class, () -> resource.update(1L, store));

    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void patchThrows422WhenNameMissing() {
    var resource = new StoreResource();
    var store = new Store("S");
    store.name = null;

    var ex = assertThrows(WebApplicationException.class, () -> resource.patch(1L, store));

    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void runAfterCommitRunsActionOnlyOnCommittedStatus() throws Exception {
    var resource = new StoreResource();
    var txRegistry = new StubTransactionSynchronizationRegistry();
    resource.transactionSynchronizationRegistry = txRegistry;

    boolean[] ran = {false};

    Method method = StoreResource.class.getDeclaredMethod("runAfterCommit", Runnable.class);
    method.setAccessible(true);
    method.invoke(resource, (Runnable) () -> ran[0] = true);

    assertFalse(ran[0]);

    txRegistry.sync.afterCompletion(Status.STATUS_ROLLEDBACK);
    assertFalse(ran[0]);

    txRegistry.sync.afterCompletion(Status.STATUS_COMMITTED);
    assertTrue(ran[0]);
  }

  private static class StubTransactionSynchronizationRegistry
      implements TransactionSynchronizationRegistry {

    private Synchronization sync;

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
      this.sync = sync;
    }

    @Override
    public Object getTransactionKey() {
      return null;
    }

    @Override
    public Object getResource(Object key) {
      return null;
    }

    @Override
    public void putResource(Object key, Object value) {}

    @Override
    public int getTransactionStatus() {
      return 0;
    }

    @Override
    public void setRollbackOnly() {}

    @Override
    public boolean getRollbackOnly() {
      return false;
    }
  }
}
