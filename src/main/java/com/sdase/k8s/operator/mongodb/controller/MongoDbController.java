package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MongoDbController implements ResourceController<MongoDbCustomResource> {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbController.class);

  private final KubernetesClient kubernetesClient;

  public MongoDbController(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  @Override
  public DeleteControl deleteResource(MongoDbCustomResource resource, Context<MongoDbCustomResource> context) {
    LOG.info(
        "MongoDb {}/{} deleted",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    return DeleteControl.DEFAULT_DELETE;
  }

  @Override
  public UpdateControl<MongoDbCustomResource> createOrUpdateResource(MongoDbCustomResource resource, Context<MongoDbCustomResource> context) {
    LOG.info(
        "MongoDb {}/{} created or updated",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    var secret = createSecretForOwner(resource);
    kubernetesClient.secrets().inNamespace(resource.getMetadata().getNamespace()).create(secret);
    return UpdateControl.noUpdate();
  }

  // TODO all below this line should be in a separate service

  private Secret createSecretForOwner(MongoDbCustomResource resource) {
    var secret = new Secret();
    secret.setData(
        Map.of(
            resource.getSpec().getSecret().getUsernameKey(),
            base64(resource.getMetadata().getName()),
            resource.getSpec().getSecret().getPasswordKey(),
            base64(UUID.randomUUID().toString())));
    ObjectMeta secretMetadata = createMetaDataFromOwnerResource(resource);
    secret.setMetadata(secretMetadata);
    return secret;
  }

  private ObjectMeta createMetaDataFromOwnerResource(MongoDbCustomResource resource) {
    var secretMetadata = new ObjectMeta();
    secretMetadata.setName(resource.getMetadata().getName());
    secretMetadata.setNamespace(resource.getMetadata().getNamespace());
    secretMetadata.setOwnerReferences(List.of(createOwnerReference(resource)));
    return secretMetadata;
  }

  private OwnerReference createOwnerReference(MongoDbCustomResource resource) {
    var ownerReference = new OwnerReference();
    ownerReference.setApiVersion(resource.getApiVersion());
    ownerReference.setKind(resource.getKind());
    ownerReference.setName(resource.getMetadata().getName());
    ownerReference.setBlockOwnerDeletion(false);
    ownerReference.setUid(resource.getMetadata().getUid());
    return ownerReference;
  }

  private static String base64(String in) {
    return Base64.getEncoder().encodeToString(in.getBytes(StandardCharsets.UTF_8));
  }
}
