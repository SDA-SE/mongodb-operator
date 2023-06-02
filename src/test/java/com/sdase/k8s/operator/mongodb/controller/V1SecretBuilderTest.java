package com.sdase.k8s.operator.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.mongodb.ConnectionString;
import com.sdase.k8s.operator.mongodb.controller.tasks.CreateDatabaseTask;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.ConnectionStringUtil;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.NamingUtil;
import com.sdase.k8s.operator.mongodb.model.v1beta1.DatabaseSpec;
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
  private static final ConnectionString MONGODB_OPERATOR_CONNECTION_STRING =
      new ConnectionString(
          "mongodb://"
              + "mongodb-operator:suer-s3cr35"
              + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
              + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
              + "/admin");

  V1SecretBuilder builder = new V1SecretBuilder();

  @Test
  void shouldUseNameAndNamespaceFromOwner() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);

    assertThat(actual.getMetadata())
        .extracting("name", "namespace")
        .containsExactly("test-db", "my-namespace");
  }

  @Test
  void shouldSetOwner() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);

    assertThat(actual.getMetadata().getOwnerReferences())
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

    assertThat(Base64.getDecoder().decode(actual.getData().get("username")))
        .isEqualTo("my-namespace_test-db".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateUniqueUsernameAndPlaceItInConfiguredDataKey() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.source().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);

    assertThat(Base64.getDecoder().decode(actual.getData().get("u")))
        .isEqualTo("my-namespace_test-db".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreatePassword() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);

    assertThat(Base64.getDecoder().decode(actual.getData().get("password")))
        .isEqualTo(PLAIN_TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateConnectionString() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.source().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);

    assertThat(Base64.getDecoder().decode(actual.getData().get("c")))
        .isEqualTo(
            ConnectionStringUtil.createConnectionString(
                    given.databaseName(),
                    given.username(),
                    given.password(),
                    "readPreference=secondaryPreferred&retryWrites=false",
                    MONGODB_OPERATOR_CONNECTION_STRING)
                .getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreatePasswordAndPlaceItInConfiguredPasswordKey() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.source().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);

    assertThat(Base64.getDecoder().decode(actual.getData().get("p")))
        .isEqualTo(PLAIN_TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateDatabaseName() {
    var given = taskWithMongoDbTestDbInMyNamespace();

    var actual = builder.createSecretForOwner(given);

    assertThat(Base64.getDecoder().decode(actual.getData().get("database")))
        .isEqualTo("my-namespace_test-db".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldCreateDatabaseAndPlaceItInConfiguredPasswordKey() {
    var given = taskWithMongoDbTestDbInMyNamespace();
    given.source().getSpec().setSecret(secretSpecWithShortenedKeys());

    var actual = builder.createSecretForOwner(given);

    assertThat(Base64.getDecoder().decode(actual.getData().get("d")))
        .isEqualTo("my-namespace_test-db".getBytes(StandardCharsets.UTF_8));
  }

  private CreateDatabaseTask taskWithMongoDbTestDbInMyNamespace() {
    var objectMeta = new ObjectMeta();
    objectMeta.setName("test-db");
    objectMeta.setNamespace("my-namespace");
    objectMeta.setUid(TEST_DB_UID);
    var mongoDbCustomResource = new MongoDbCustomResource();
    mongoDbCustomResource.setMetadata(objectMeta);
    mongoDbCustomResource
        .getSpec()
        .setDatabase(
            new DatabaseSpec()
                .setConnectionStringOptions("readPreference=secondaryPreferred&retryWrites=false"));

    return TaskFactory.customFactory(
            NamingUtil::fromNamespaceAndName,
            mdbCr -> PLAIN_TEST_PASSWORD,
            NamingUtil::fromNamespaceAndName)
        .newCreateTask(mongoDbCustomResource, MONGODB_OPERATOR_CONNECTION_STRING);
  }

  private SecretSpec secretSpecWithShortenedKeys() {
    return new SecretSpec()
        .setDatabaseKey("d")
        .setUsernameKey("u")
        .setPasswordKey("p")
        .setConnectionStringKey("c");
  }
}
