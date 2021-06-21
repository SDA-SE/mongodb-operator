package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.KubernetesClientAdapter;
import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.controller.V1SecretBuilder;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import java.util.UUID;

public class MongoDbOperator {

  public static void main(String[] args) {

    var config = new EnvironmentConfig();
    var mongoDbService = new MongoDbService(config.getMongodbConnectionString());

    try (var client = new DefaultKubernetesClient();
        var operator = new Operator(client, DefaultConfigurationService.instance())) {
      operator.register(
          new MongoDbController(
              new KubernetesClientAdapter(client),
              new V1SecretBuilder(() -> UUID.randomUUID().toString())));
      new KeepAliveRunner().keepAlive();
    }
  }
}
