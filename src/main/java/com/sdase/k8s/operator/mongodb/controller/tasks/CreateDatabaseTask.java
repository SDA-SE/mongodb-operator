package com.sdase.k8s.operator.mongodb.controller.tasks;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;

public class CreateDatabaseTask {

  private final MongoDbCustomResource source;
  private final String databaseName;
  private final String username;
  private final String password;
  private final String connectionString;

  public CreateDatabaseTask(
      MongoDbCustomResource source,
      String databaseName,
      String username,
      String password,
      String connectionString) {
    this.source = source;
    this.databaseName = databaseName;
    this.username = username;
    this.password = password;
    this.connectionString = connectionString;
  }

  public MongoDbCustomResource getSource() {
    return source;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getConnectionString() {
    return connectionString;
  }
}
