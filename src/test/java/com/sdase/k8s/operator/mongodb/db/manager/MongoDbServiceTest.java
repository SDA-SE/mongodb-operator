package com.sdase.k8s.operator.mongodb.db.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
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
}
