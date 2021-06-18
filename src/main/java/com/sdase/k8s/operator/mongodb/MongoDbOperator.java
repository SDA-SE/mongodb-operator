package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbOperator {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbOperator.class);

  private static final Object WAITER = new Object();

  public static void main(String[] args) {

    var config = new EnvironmentConfig();
    var mongoDbService = new MongoDbService(config.getMongodbConnectionString());

    KubernetesClient client = new DefaultKubernetesClient();
    try (var operator = new Operator(client, DefaultConfigurationService.instance())) {
      operator.register(new MongoDbController(client));
      keepAlive();
    }
  }

  private static void keepAlive() {
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
