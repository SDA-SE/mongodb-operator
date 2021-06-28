package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.controller.tasks.CreateDatabaseTask;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class V1SecretBuilder {

  Secret createSecretForOwner(CreateDatabaseTask createDatabaseTask) {
    var secret = new Secret();
    var owner = createDatabaseTask.getSource();
    var database = createDatabaseTask.getDatabaseName();
    var username = createDatabaseTask.getUsername();
    var password = createDatabaseTask.getPassword();
    secret.setData(
        Map.of(
            owner.getSpec().getSecret().getDatabaseKey(),
            base64(database),
            owner.getSpec().getSecret().getUsernameKey(),
            base64(username),
            owner.getSpec().getSecret().getPasswordKey(),
            base64(password)));
    ObjectMeta secretMetadata = createMetaDataFromOwnerResource(owner);
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
