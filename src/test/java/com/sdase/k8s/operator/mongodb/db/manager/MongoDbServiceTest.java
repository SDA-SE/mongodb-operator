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
    var actual = mongoDbService.createDatabaseWithUser("test-db", UUID.randomUUID().toString());

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldNotCreateUserThatAlreadyExists() {
    // given
    mongoDbService.createDatabaseWithUser("existing-test-db", UUID.randomUUID().toString());

    // when
    var actual =
        mongoDbService.createDatabaseWithUser("existing-test-db", UUID.randomUUID().toString());

    // then
    assertThat(actual).isFalse();
  }

  @Test
  void shouldDropUser() {
    // given
    mongoDbService.createDatabaseWithUser("test-db", UUID.randomUUID().toString());

    // when
    var actual = mongoDbService.dropDatabaseUser("test-db", "test-db");

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldConfirmIfUserDidNotExist() {
    // given … nothing

    // when
    var actual = mongoDbService.dropDatabaseUser("test-db", "test-db");

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldDropDatabase() {
    // given
    createDb("test-db-to-drop");

    // when
    var actual = mongoDbService.dropDatabase("test-db-to-drop");

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldConfirmIfDbDidNotExist() {
    // given … nothing

    // when
    var actual = mongoDbService.dropDatabase("test-db-to-drop");

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldIdentifyExistingUser() {
    // given
    mongoDbService.createDatabaseWithUser("test-db", UUID.randomUUID().toString());

    // when
    var actual = mongoDbService.userExists("test-db", "test-db");

    // then
    assertThat(actual).isTrue();
  }

  @Test
  void shouldNotIdentifyAbsentUser() {
    // given
    mongoDbService.createDatabaseWithUser("test-db1", UUID.randomUUID().toString());
    mongoDbService.createDatabaseWithUser("test-db2", UUID.randomUUID().toString());

    // when
    var actual = mongoDbService.userExists("test-db3", "test-db3");

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
