package com.sdase.k8s.operator.mongodb.controller.tasks;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;

public class DeleteDatabaseTask {

  private final MongoDbCustomResource source;
  private final String databaseName;
  private final String username;

  public DeleteDatabaseTask(MongoDbCustomResource source, String databaseName, String username) {
    this.source = source;
    this.databaseName = databaseName;
    this.username = username;
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
}
