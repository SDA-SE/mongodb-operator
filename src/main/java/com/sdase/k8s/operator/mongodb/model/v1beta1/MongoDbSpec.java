package com.sdase.k8s.operator.mongodb.model.v1beta1;

public class MongoDbSpec {

  private DatabaseSpec database = new DatabaseSpec();

  private SecretSpec secret = new SecretSpec();

  public DatabaseSpec getDatabase() {
    return database;
  }

  public MongoDbSpec setDatabase(DatabaseSpec database) {
    this.database = database;
    return this;
  }

  public SecretSpec getSecret() {
    return secret;
  }

  public MongoDbSpec setSecret(SecretSpec secret) {
    this.secret = secret;
    return this;
  }
}
