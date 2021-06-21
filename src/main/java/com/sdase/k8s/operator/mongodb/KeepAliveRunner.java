package com.sdase.k8s.operator.mongodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The operator is watching Kubernetes in a separated thread so we must make sure, that the
 * application stays running and does not end with the main method.
 */
class KeepAliveRunner {

  private static final Logger LOG = LoggerFactory.getLogger(KeepAliveRunner.class);
  private static final Object WAITER = new Object();

  void keepAlive() {
    synchronized (WAITER) {
      try {
        //noinspection InfiniteLoopStatement
        while (true) { // NOSONAR
          WAITER.wait();
        }
      } catch (InterruptedException e) {
        LOG.info("Got interrupted.", e);
        Thread.currentThread().interrupt();
      }
      LOG.info("Exiting");
    }
  }
}
