package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.KubernetesClientAdapter;
import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.controller.V1SecretBuilder;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;

public class MongoDbOperator {

  public MongoDbOperator(KubernetesClient kubernetesClient) {
    try (var operator = new Operator(kubernetesClient, DefaultConfigurationService.instance())) {
      operator.register(createMongoDbController(kubernetesClient));
      keepRunning();
    }
  }

  private MongoDbController createMongoDbController(KubernetesClient kubernetesClient) {
    var config = new EnvironmentConfig();
    var mongoDbService = new MongoDbService(config.getMongodbConnectionString());
    return new MongoDbController(
        new KubernetesClientAdapter(kubernetesClient),
        TaskFactory.defaultFactory(),
        mongoDbService,
        new V1SecretBuilder());
  }

  private void keepRunning() {
    new KeepAliveRunner().keepAlive();
  }

  public static void main(String[] args) {
    try (var client = new DefaultKubernetesClient()) {
      new MongoDbOperator(client);
    }
  }
}
