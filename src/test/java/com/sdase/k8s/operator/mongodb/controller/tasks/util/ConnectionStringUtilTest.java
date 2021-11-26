package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ConnectionString;
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
}
