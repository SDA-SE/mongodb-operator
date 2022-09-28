package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ConnectionString;
import java.net.URI;
import org.junit.jupiter.api.Test;

class ConnectionStringUtilTest {

  @Test
  void shouldReplaceDatabaseUserAndPassword() {
    var given =
        "mongodb://"
            + "mongodb-operator:suer-s3cr35"
            + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
            + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
            + "/admin"
            + "?ssl=true&replicaSet=rs0&readpreference=secondaryPreferred";
    var username = "my-service-user";
    var password = "SHOULD_%_ESCAPE/ME";
    var database = "my-service-db";
    var connectionString = new ConnectionString(given);
    var actual =
        ConnectionStringUtil.createConnectionString(
            database, username, password, null, connectionString);
    // verify connection string is valid
    assertThat(actual)
        .isEqualTo(
            "mongodb://"
                + "my-service-user:SHOULD_%25_ESCAPE%2FME"
                + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
                + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
                + "/my-service-db"
                + "?ssl=true&replicaSet=rs0&readpreference=secondaryPreferred");
  }

  @Test
  void shouldReplaceCustomOptions() {
    var given =
        "mongodb://"
            + "mongodb-operator:suer-s3cr35"
            + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
            + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
            + "/admin"
            + "?ssl=true";
    var username = "my-service-user";
    var password = "dummy";
    var database = "my-service-db";
    var connectionString = new ConnectionString(given);
    var actual =
        ConnectionStringUtil.createConnectionString(
            database,
            username,
            password,
            "replicaSet=rs0&readpreference=secondaryPreferred",
            connectionString);
    // verify connection string is valid
    assertThat(actual)
        .isEqualTo(
            "mongodb://"
                + "my-service-user:dummy"
                + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
                + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
                + "/my-service-db"
                + "?replicaSet=rs0&readpreference=secondaryPreferred");
  }

  @Test
  void shouldIgnoreOptionsIfOptionsAreNotSet() {
    var given =
        "mongodb://"
            + "mongodb-operator:suer-s3cr35"
            + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
            + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
            + "/admin";
    var username = "my-service-user";
    var password = "dummy";
    var database = "my-service-db";
    var connectionString = new ConnectionString(given);
    var actual =
        ConnectionStringUtil.createConnectionString(
            database, username, password, null, connectionString);
    // verify connection string is valid
    assertThat(actual)
        .isEqualTo(
            "mongodb://"
                + "my-service-user:dummy"
                + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
                + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
                + "/my-service-db");
  }

  @Test
  void shouldSupportMongoDbSrvSchema() {
    var given = "mongodb+srv://server.example.com/admin?connectTimeoutMS=2000";
    var username = "my-service-user";
    var password = "dummy";
    var database = "my-service-db";
    var connectionString = new ConnectionString(given);
    var actual =
        ConnectionStringUtil.createConnectionString(
            database, username, password, null, connectionString);
    // verify connection string is valid
    assertThat(actual)
        .isEqualTo(
            "mongodb+srv://"
                + "my-service-user:dummy"
                + "@server.example.com"
                + "/my-service-db?connectTimeoutMS=2000");
  }

  /**
   * A test to clarify what can be done with {@link URI} and the {@link ConnectionString} as used in
   * {@link ConnectionStringUtil#createConnectionString(String, String, String, String,
   * ConnectionString)}. The {@link ConnectionString} does not provide a getter for the query part
   * but for all individual properties. But {@link URI} can be used to get it. However, you can't
   * get the host from an {@link URI} when the connection string contains multiple host definitions.
   */
  @Test
  void clarifyCapabilitiesOfUri() {
    URI actual = URI.create("mongodb-srv://foo1.bar:1234,foo2.bar:1234/path?foo=bar");
    assertThat(actual.getScheme()).isEqualTo("mongodb-srv");
    assertThat(actual.getPath()).isEqualTo("/path");
    assertThat(actual.getQuery()).isEqualTo("foo=bar");
    assertThat(actual).hasNoHost(); // URI parses all but the host correctly
  }
}
