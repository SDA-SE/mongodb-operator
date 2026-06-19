package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.controller.tasks.CreateDatabaseTask;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.SecretSpec;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class V1SecretBuilder {

  Secret createSecretForOwner(CreateDatabaseTask createDatabaseTask) {
    var secret = new Secret();
    var owner = createDatabaseTask.source();
    var database = createDatabaseTask.databaseName();
    var username = createDatabaseTask.username();
    var password = createDatabaseTask.password();
    var connectionString = createDatabaseTask.connectionString();
    var secretSpec = Optional.ofNullable(owner.getSpec()).map(MongoDbSpec::getSecret);
    secret.setData(
        Map.of(
            secretSpec.map(SecretSpec::getDatabaseKey).orElse(SecretSpec.DEFAULT_DATABASE_KEY),
            base64(database),
            secretSpec.map(SecretSpec::getUsernameKey).orElse(SecretSpec.DEFAULT_USERNAME_KEY),
            base64(username),
            secretSpec.map(SecretSpec::getPasswordKey).orElse(SecretSpec.DEFAULT_PASSWORD_KEY),
            base64(password),
            secretSpec
                .map(SecretSpec::getConnectionStringKey)
                .orElse(SecretSpec.DEFAULT_CONNECTION_STRING_KEY),
            base64(connectionString)));
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
