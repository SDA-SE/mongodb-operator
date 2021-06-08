package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDb;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbOperator {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbOperator.class);

  public static void main(String[] args) {
    try (KubernetesClient client = new DefaultKubernetesClient()) {
      String namespace = client.getNamespace();
      if (namespace == null) {
        LOG.info("No namespace found via config, assuming default.");
        namespace = "default";
      }

      LOG.info("Using namespace {}", namespace);

      SharedInformerFactory informerFactory = client.informers();

      SharedIndexInformer<MongoDb> mongoDbSharedIndexInformer =
          informerFactory.sharedIndexInformerFor(MongoDb.class, MongoDbList.class, 10L * 60 * 1000);
      MixedOperation<MongoDb, MongoDbList, Resource<MongoDb>> mixedOperation =
          client.customResources(MongoDb.class, MongoDbList.class);
      MongoDbController mongoDbController =
          new MongoDbController(client, mongoDbSharedIndexInformer, mixedOperation, namespace);

      mongoDbController.create();
      informerFactory.startAllRegisteredInformers();
      informerFactory.addSharedInformerEventListener(
          e -> LOG.warn("Exception occurred, but caught.", e));

      mongoDbController.run();
    } catch (KubernetesClientException e) {
      LOG.error("Kubernetes Client Exception: {}", e.getMessage(), e);
    }
  }
}
