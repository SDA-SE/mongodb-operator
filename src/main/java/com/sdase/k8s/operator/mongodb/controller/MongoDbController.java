package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDb;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbController {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbController.class);

  private final KubernetesClient kubernetesClient;
  private final SharedIndexInformer<MongoDb> mongoDbInformer;
  private final MixedOperation<MongoDb, MongoDbList, Resource<MongoDb>> mongoDbClient;
  private final Lister<MongoDb> mongoDbLister;
  private final BlockingQueue<String> workQueue;

  public MongoDbController(
      KubernetesClient kubernetesClient,
      SharedIndexInformer<MongoDb> mongoDbInformer,
      MixedOperation<MongoDb, MongoDbList, Resource<MongoDb>> mongoDbClient,
      String namespace) {
    this.kubernetesClient = kubernetesClient;
    this.mongoDbInformer = mongoDbInformer;
    this.mongoDbClient = mongoDbClient;
    this.mongoDbLister = new Lister<>(mongoDbInformer.getIndexer(), namespace);
    this.workQueue = new ArrayBlockingQueue<>(1024);
  }

  public void create() {
    mongoDbInformer.addEventHandler(new MongoDbEventHandler());
  }

  public void run() {
    LOG.info("Starting MongoDb controller");
    while (!mongoDbInformer.hasSynced()) {
      // Wait till Informer syncs
    }

    while (true) {
      try {
        LOG.info("trying to fetch item from work queue...");
        if (workQueue.isEmpty()) {
          LOG.info("Work Queue is empty");
        }
        String key = workQueue.take();
        Objects.requireNonNull(key, "key can't be null");
        LOG.info("Got {}", key);
        if (key.isEmpty() || (!key.contains("/"))) {
          LOG.warn("invalid resource key: {}", key);
        }

        // Get the resource's name from key which is in format namespace/name
        String name = key.split("/")[1];
        MongoDb mongoDb = mongoDbLister.get(name);
        if (mongoDb == null) {
          LOG.warn("MongoDb {} in work queue no longer exists", name);
          return;
        }
        // reconcile(podSet);

      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        LOG.error("controller interrupted.");
      }
    }
  }
}
