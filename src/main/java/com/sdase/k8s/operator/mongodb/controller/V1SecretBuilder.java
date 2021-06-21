package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class V1SecretBuilder {

  private final Supplier<String> passwordCreator;

  public V1SecretBuilder(Supplier<String> passwordCreator) {
    this.passwordCreator = passwordCreator;
  }

  SecretHolder createSecretForOwner(MongoDbCustomResource owner) {
    var secret = new Secret();
    var ownerMetadata = owner.getMetadata();
    var username = String.join("_", ownerMetadata.getNamespace(), ownerMetadata.getName());
    var password = passwordCreator.get();
    secret.setData(
        Map.of(
            owner.getSpec().getSecret().getUsernameKey(),
            base64(username),
            owner.getSpec().getSecret().getPasswordKey(),
            base64(password)));
    ObjectMeta secretMetadata = createMetaDataFromOwnerResource(owner);
    secret.setMetadata(secretMetadata);
    return new SecretHolder(username, password, secret);
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

  public static class SecretHolder {

    private final String plainUsername;
    private final String plainPassword;
    private final Secret secret;

    public SecretHolder(String plainUsername, String plainPassword, Secret secret) {
      this.plainUsername = plainUsername;
      this.plainPassword = plainPassword;
      this.secret = secret;
    }

    public String getPlainUsername() {
      return plainUsername;
    }

    public String getPlainPassword() {
      return plainPassword;
    }

    public Secret getSecret() {
      return secret;
    }
  }
}
