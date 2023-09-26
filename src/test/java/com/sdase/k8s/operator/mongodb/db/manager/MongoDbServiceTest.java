package com.sdase.k8s.operator.mongodb.db.manager;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import com.mongodb.MongoClient;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService.CreateDatabaseResult;
import com.sdase.k8s.operator.mongodb.db.manager.model.User;
import com.sdase.k8s.operator.mongodb.ssl.CertificateCollector;
import com.sdase.k8s.operator.mongodb.ssl.util.SslUtil;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

abstract class MongoDbServiceTest extends AbstractMongoDbTest {

  static class MongoDb40Test extends MongoDbServiceTest {
    @BeforeAll
    static void beforeAll() {
      startDb(Version.Main.V4_0);
    }
  }

  @DisabledIfEnvironmentVariable(
      named = OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME,
      matches = ".+",
      disabledReason = "Don't execute multiple times with external database.")
  static class MongoDb42Test extends MongoDbServiceTest {
    @BeforeAll
    static void beforeAll() {
      startDb(Version.Main.V4_2);
    }
  }

  @DisabledIfEnvironmentVariable(
      named = OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME,
      matches = ".+",
      disabledReason = "Don't execute multiple times with external database.")
  static class MongoDb44Test extends MongoDbServiceTest {
    @BeforeAll
    static void beforeAll() {
      startDb(Version.Main.V4_4);
    }
  }

  @DisabledIfEnvironmentVariable(
      named = OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME,
      matches = ".+",
      disabledReason = "Don't execute multiple times with external database.")
  static class MongoDb50Test extends MongoDbServiceTest {
    @BeforeAll
    static void beforeAll() {
      startDb(Version.Main.V5_0);
    }
  }

  @DisabledIfEnvironmentVariable(
      named = OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME,
      matches = ".+",
      disabledReason = "Don't execute multiple times with external database.")
  static class MongoDb60Test extends MongoDbServiceTest {
    @BeforeAll
    static void beforeAll() {
      startDb(Version.Main.V6_0);
    }
  }

  @DisabledIfEnvironmentVariable(
      named = OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME,
      matches = ".+",
      disabledReason = "Don't execute multiple times with external database.")
  static class MongoDb70Test extends MongoDbServiceTest {
    @BeforeAll
    static void beforeAll() {
      startDb(Version.Main.V7_0);
    }
  }

  private final MongoDbService mongoDbService = new MongoDbService(getMongoDbConnectionString());

  @AfterAll
  static void afterAll() {
    stopDb();
  }

  @BeforeEach
  void clean() {
    removeDatabases();
  }

  @AfterEach
  void verifyUsersAreCleanedUpInDocumentDb() {
    if (!mongoDbService.checkDocumentDb(mongoDbService.getConnectionString())) {
      return;
    }
    long count = -1; // not zero -> fail
    try (var mongoClient = new MongoClient(getMongoDbConnectionString())) {
      Document dbStats = new Document("usersInfo", 1);
      Document command = mongoClient.getDatabase("admin").runCommand(dbStats);
      var usersResult = command.get("users");
      if (usersResult instanceof ArrayList<?> users) {
        count =
            users.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .map(d -> d.get("user"))
                .filter(Objects::nonNull)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(
                    u ->
                        u.startsWith("test-db-")
                            || u.startsWith("existing-test-db-")
                            || u.startsWith("test-db1-")
                            || u.startsWith("test-db2-"))
                .count();
      }
      assertThat(count)
          .describedAs(
              "Counted %d test users, should be zero but allowing up to 20 to cover parallel jobs.",
              count)
          .isNotNegative()
          .isLessThanOrEqualTo(20);
    }
  }

  @Test
  void shouldCreateUser() {
    var databaseAndUserName = registerTestDb("test-db");
    try {
      // given … nothing

      // when
      var actual =
          mongoDbService.createDatabaseWithUser(databaseAndUserName, UUID.randomUUID().toString());

      // then
      assertThat(actual).isEqualTo(CreateDatabaseResult.CREATED);
    } finally {
      mongoDbService.dropDatabaseUser(databaseAndUserName, databaseAndUserName);
    }
  }

  @Test
  void shouldNotCreateUserThatAlreadyExists() {
    var databaseAndUserName = registerTestDb("existing-test-db");
    try {
      // given
      mongoDbService.createDatabaseWithUser(databaseAndUserName, UUID.randomUUID().toString());

      // when
      var actual =
          mongoDbService.createDatabaseWithUser(databaseAndUserName, UUID.randomUUID().toString());

      // then
      assertThat(actual).isEqualTo(CreateDatabaseResult.SKIPPED);
    } finally {
      mongoDbService.dropDatabaseUser(databaseAndUserName, databaseAndUserName);
    }
  }

  @Test
  void shouldDropUser() {
    var databaseAndUserName = registerTestDb("test-db-to-be-dropped");
    try {
      // given
      mongoDbService.createDatabaseWithUser(databaseAndUserName, UUID.randomUUID().toString());

      // when
      var actual = mongoDbService.dropDatabaseUser(databaseAndUserName, databaseAndUserName);

      // then
      assertThat(actual).isTrue();
    } finally {
      mongoDbService.dropDatabaseUser(databaseAndUserName, databaseAndUserName);
    }
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
    var databaseAndUserName = registerTestDb("test-db-existing-user");
    try {
      // given
      mongoDbService.createDatabaseWithUser(databaseAndUserName, UUID.randomUUID().toString());

      // when
      var actual = mongoDbService.userExists(databaseAndUserName, databaseAndUserName);

      // then
      assertThat(actual).isTrue();
    } finally {
      mongoDbService.dropDatabaseUser(databaseAndUserName, databaseAndUserName);
    }
  }

  @Test
  void shouldNotIdentifyAbsentUser() {
    var databaseAndUserNameOne = registerTestDb("test-db1");
    var databaseAndUserNameTwo = registerTestDb("test-db2");
    try {
      // given
      mongoDbService.createDatabaseWithUser(databaseAndUserNameOne, UUID.randomUUID().toString());
      mongoDbService.createDatabaseWithUser(databaseAndUserNameTwo, UUID.randomUUID().toString());

      // when
      var actual =
          mongoDbService.userExists(registerTestDb("test-db3"), registerTestDb("test-db3"));

      // then
      assertThat(actual).isFalse();
    } finally {
      mongoDbService.dropDatabaseUser(databaseAndUserNameOne, databaseAndUserNameOne);
      mongoDbService.dropDatabaseUser(databaseAndUserNameTwo, databaseAndUserNameTwo);
    }
  }

  @Test
  void shouldFailOnInvalidDatabaseName() {
    // given … nothing

    // when
    var actual =
        mongoDbService.createDatabaseWithUser(
            "invalid-/\\. \"$*<>:|?-db", UUID.randomUUID().toString());

    // then
    assertThat(actual).isEqualTo(CreateDatabaseResult.FAILED);
  }

  @Test
  void shouldKeepUseSslFromConnectionStringSoThatCaCertificatesCanBeAddedInAdvance()
      throws URISyntaxException {

    var givenPathToDirectoryWithCertificates =
        Path.of(requireNonNull(getClass().getResource("/ssl")).toURI()).toString();

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

  @Test
  void shouldFindOutWhoIAm() {

    var user = mongoDbService.whoAmI();

    assertThat(user)
        .isPresent()
        .hasValueSatisfying(
            u -> {
              // not checking exact values, just proof the mapping
              assertThat(u.getId()).isNotBlank();
              assertThat(u.getUsername()).isNotBlank();
              assertThat(u.getRoles())
                  .extracting(User.UserRole::role, User.UserRole::db)
                  .containsAnyOf(tuple("userAdminAnyDatabase", "admin"), tuple("root", "admin"));
            });
  }
}
