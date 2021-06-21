package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MongoDbController implements ResourceController<MongoDbCustomResource> {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbController.class);

  private final KubernetesClient kubernetesClient;
  private final V1SecretBuilder v1SecretBuilder;

  public MongoDbController(KubernetesClient kubernetesClient, V1SecretBuilder v1SecretBuilder) {
    this.kubernetesClient = kubernetesClient;
    this.v1SecretBuilder = v1SecretBuilder;
  }

  @Override
  public DeleteControl deleteResource(
      MongoDbCustomResource resource, Context<MongoDbCustomResource> context) {
    LOG.info(
        "MongoDb {}/{} deleted",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    return DeleteControl.DEFAULT_DELETE;
  }

  @Override
  public UpdateControl<MongoDbCustomResource> createOrUpdateResource(
      MongoDbCustomResource resource, Context<MongoDbCustomResource> context) {
    LOG.info(
        "MongoDb {}/{} created or updated",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    var secret = v1SecretBuilder.createSecretForOwner(resource);
    kubernetesClient
        .secrets()
        .inNamespace(resource.getMetadata().getNamespace())
        .create(secret.getSecret());
    return UpdateControl.noUpdate();
  }
}
