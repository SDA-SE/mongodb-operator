package com.sdase.k8s.operator.mongodb;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The operator is watching Kubernetes in a separated thread so we must make sure, that the
 * application stays running and does not end with the main method.
 */
class KeepAliveRunner {

  private static final Logger LOG = LoggerFactory.getLogger(KeepAliveRunner.class);
  private static final Object WAITER = new Object();
  private final List<AutoCloseable> toBeClosedAfterFinish = new ArrayList<>();

  public KeepAliveRunner() {}

  public KeepAliveRunner(AutoCloseable... toBeClosedAfterFinish) {
    this.toBeClosedAfterFinish.addAll(List.of(toBeClosedAfterFinish));
  }

  void keepAlive() {
    synchronized (WAITER) {
      try {
        //noinspection InfiniteLoopStatement
        while (true) { // NOSONAR
          WAITER.wait();
        }
      } catch (InterruptedException e) {
        LOG.info("Got interrupted.", e);
        closeAll();
        Thread.currentThread().interrupt();
      }
      LOG.info("Exiting");
    }
  }

  private void closeAll() {
    toBeClosedAfterFinish.forEach(
        c -> {
          try {
            c.close();
          } catch (Exception e) {
            LOG.warn("Closing after finish threw exception", e);
          }
        });
  }
}
