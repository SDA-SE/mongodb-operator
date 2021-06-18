package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDb;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbOperator {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbOperator.class);

  public static void main(String[] args) {

    var config = new EnvironmentConfig();
    var mongoDbService = new MongoDbService(config.getMongodbConnectionString());

    try (KubernetesClient client = new DefaultKubernetesClient()) {
      buildAndRunController(client);
    } catch (KubernetesClientException e) {
      LOG.error("Kubernetes Client Exception: {}", e.getMessage(), e);
    }
  }

  private static void buildAndRunController(KubernetesClient client) {
    String namespace = determineNamespace(client);
    SharedInformerFactory informerFactory = client.informers();

    var mongoDbSharedIndexInformer =
        informerFactory.sharedIndexInformerFor(MongoDb.class, MongoDbList.class, 10L * 60 * 1000);
    var mixedOperation = client.customResources(MongoDb.class, MongoDbList.class);
    var mongoDbController =
        new MongoDbController(client, mongoDbSharedIndexInformer, mixedOperation, namespace);

    mongoDbController.create();
    informerFactory.startAllRegisteredInformers();
    informerFactory.addSharedInformerEventListener(
        e -> LOG.warn("Exception occurred, but caught.", e));

    mongoDbController.run();
  }

  private static String determineNamespace(KubernetesClient client) {
    String namespace = client.getNamespace();
    if (namespace == null) {
      LOG.info("No namespace found via config, assuming default.");
      namespace = "default";
    }

    LOG.info("Using namespace {}", namespace);
    return namespace;
  }
}
