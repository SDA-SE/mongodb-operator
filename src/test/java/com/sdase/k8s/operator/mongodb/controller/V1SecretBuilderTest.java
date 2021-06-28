package com.sdase.k8s.operator.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sdase.k8s.operator.mongodb.controller.tasks.CreateDatabaseTask;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.NamingUtil;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.SecretSpec;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class V1SecretBuilderTest {

  private static final String TEST_DB_UID = UUID.randomUUID().toString();
  private static final String PLAIN_TEST_PASSWORD = "static-test-password";

  V1SecretBuilder builder = new V1SecretBuilder();

  @Test
  void shouldUseNameAndNamespaceFromOwner() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(actualSecret.getMetadata())
        .extracting("name", "namespace")
        .containsExactly("test-db", "my-namespace");
  }

  @Test
  void shouldSetOwner() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(actualSecret.getMetadata().getOwnerReferences())
        .extracting(
            OwnerReference::getApiVersion,
            OwnerReference::getKind,
            OwnerReference::getName,
            OwnerReference::getUid,
            OwnerReference::getBlockOwnerDeletion)
        .containsExactly(
            tuple("persistence.sda-se.com/v1beta1", "MongoDb", "test-db", TEST_DB_UID, false));
  }

  @Test
  void shouldCreateUniqueUsername() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(actual.getPlainUsername()).isEqualTo("my-namespace_test-db");
    assertThat(Base64.getDecoder().decode(actualSecret.getData().get("username")))
        .isEqualTo(actual.getPlainUsername().getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateUniqueUsernameAndPlaceItInConfiguredDataKey() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.getSource().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(actual.getPlainUsername()).isEqualTo("my-namespace_test-db");
    assertThat(Base64.getDecoder().decode(actualSecret.getData().get("u")))
        .isEqualTo(actual.getPlainUsername().getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreatePassword() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(actual.getPlainPassword()).isEqualTo(PLAIN_TEST_PASSWORD);
    assertThat(Base64.getDecoder().decode(actualSecret.getData().get("password")))
        .isEqualTo(actual.getPlainPassword().getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreatePasswordAndPlaceItInConfiguredPasswordKey() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.getSource().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(actual.getPlainPassword()).isEqualTo(PLAIN_TEST_PASSWORD);
    assertThat(Base64.getDecoder().decode(actualSecret.getData().get("p")))
        .isEqualTo(actual.getPlainPassword().getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateDatabaseName() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(Base64.getDecoder().decode(actualSecret.getData().get("database")))
        .isEqualTo("my-namespace_test-db".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateDatabaseAndPlaceItInConfiguredPasswordKey() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.getSource().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);
    var actualSecret = actual.getSecret();

    assertThat(Base64.getDecoder().decode(actualSecret.getData().get("d")))
        .isEqualTo("my-namespace_test-db".getBytes(StandardCharsets.UTF_8));
  }

  private CreateDatabaseTask taskWithMongoDbTestDbInMyNamespace() {
    var objectMeta = new ObjectMeta();
    objectMeta.setName("test-db");
    objectMeta.setNamespace("my-namespace");
    objectMeta.setUid(TEST_DB_UID);
    var mongoDbCustomResource = new MongoDbCustomResource();
    mongoDbCustomResource.setMetadata(objectMeta);

    return TaskFactory.customFactory(
            NamingUtil::fromNamespaceAndName,
            mdbCr -> PLAIN_TEST_PASSWORD,
            NamingUtil::fromNamespaceAndName)
        .newCreateTask(mongoDbCustomResource);
  }

  private SecretSpec secretSpecWithShortenedKeys() {
    return new SecretSpec().setDatabaseKey("d").setUsernameKey("u").setPasswordKey("p");
  }
}
