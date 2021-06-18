package com.sdase.k8s.operator.mongodb.db.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
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
  void shouldListDatabases() {
    // given
    createDb("foo");

    // when
    var actual = mongoDbService.listDatabases();

    // then
    assertThat(actual).contains("foo");
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
  void shouldNotCreateUserForExistingDatabase() {
    // given
    createDb("existing-test-db");

    // when
    var actual =
        mongoDbService.createDatabaseWithUser("existing-test-db", UUID.randomUUID().toString());

    // then
    SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(actual).isFalse();
          softly.assertThat(mongoDbService.databaseExists("existing-test-db")).isTrue();
        });
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
