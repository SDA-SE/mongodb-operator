package com.sdase.k8s.operator.mongodb.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.sdase.k8s.operator.mongodb.ssl.CertificateCollector;
import com.sdase.k8s.operator.mongodb.ssl.util.SslUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongoDbServiceTest extends AbstractMongoDbTest {

  private final MongoDbService mongoDbService = new MongoDbService(getMongoDbConnectionString());

  @BeforeAll
  static void beforeAll() throws IOException {
    startDb();
  }

  @AfterAll
  static void afterAll() {
    stopDb();
  }

  @BeforeEach
  void clean() {
    removeDatabases();
  }

  @Test
  void shouldCreateUser() {
    // given … nothing

    // when
    var actual =
        mongoDbService.createDatabaseWithUser(
            registerTestDb("test-db"), UUID.randomUUID().toString());

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldNotCreateUserThatAlreadyExists() {
    // given
    mongoDbService.createDatabaseWithUser(
        registerTestDb("existing-test-db"), UUID.randomUUID().toString());

    // when
    var actual =
        mongoDbService.createDatabaseWithUser(
            registerTestDb("existing-test-db"), UUID.randomUUID().toString());

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldDropUser() {
    // given
    mongoDbService.createDatabaseWithUser(
        registerTestDb("test-db-to-be-dropped"), UUID.randomUUID().toString());

    // when
    var actual =
        mongoDbService.dropDatabaseUser(
            registerTestDb("test-db-to-be-dropped"), registerTestDb("test-db-to-be-dropped"));

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldConfirmIfUserDidNotExist() {
    // given … nothing

    // when
    var actual =
        mongoDbService.dropDatabaseUser(
            registerTestDb("test-db-not-existing"), registerTestDb("test-db-not-existing"));

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldDropDatabase() {
    // given
    createDb(registerTestDb("test-db-to-drop"));

    // when
    var actual = mongoDbService.dropDatabase(registerTestDb("test-db-to-drop"));

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldConfirmIfDbDidNotExist() {
    // given … nothing

    // when
    var actual = mongoDbService.dropDatabase(registerTestDb("test-db-does-not-exist"));

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldIdentifyExistingUser() {
    // given
    mongoDbService.createDatabaseWithUser(
        registerTestDb("test-db-existing-user"), UUID.randomUUID().toString());

    // when
    var actual =
        mongoDbService.userExists(
            registerTestDb("test-db-existing-user"), registerTestDb("test-db-existing-user"));

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldNotIdentifyAbsentUser() {
    // given
    mongoDbService.createDatabaseWithUser(registerTestDb("test-db1"), UUID.randomUUID().toString());
    mongoDbService.createDatabaseWithUser(registerTestDb("test-db2"), UUID.randomUUID().toString());

    // when
    var actual = mongoDbService.userExists(registerTestDb("test-db3"), registerTestDb("test-db3"));

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldFailOnInvalidDatabaseName() {
    // given … nothing

    // when
    var actual =
        mongoDbService.createDatabaseWithUser(
            "invalid-/\\. \"$*<>:|?-db", UUID.randomUUID().toString());

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldKeepUseSslFromConnectionStringSoThatCaCertificatesCanBeAddedInAdvance()
      throws URISyntaxException {

    var givenPathToDirectoryWithCertificates =
        Path.of(getClass().getResource("/ssl").toURI()).toString();

    var sslContextOptional =
        new CertificateCollector(givenPathToDirectoryWithCertificates)
            .readCertificates()
            .map(SslUtil::createTruststoreFromPemKey)
            .map(SslUtil::createSslContext);
    var sslContext = sslContextOptional.orElseGet(() -> fail("No SSL Context"));
    var service =
        sslContextOptional
            .map(sc -> new MongoDbService(getMongoDbConnectionString(), sc))
            .orElseGet(() -> fail("No SSL Context"));

    assertThat(service)
        .extracting("mongoClient")
        .extracting("settings")
        .extracting("sslSettings")
        .extracting("enabled")
        .isEqualTo(false);
    assertThat(service)
        .extracting("mongoClient")
        .extracting("settings")
        .extracting("sslSettings")
        .extracting("context")
        .isSameAs(sslContext);
  }
}
