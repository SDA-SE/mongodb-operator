package com.sdase.k8s.operator.mongodb.model.v1beta1;

public class SecretSpec {

  public static final String DEFAULT_DATABASE_KEY = "database";
  public static final String DEFAULT_USERNAME_KEY = "username";
  public static final String DEFAULT_PASSWORD_KEY = "password";
  public static final String DEFAULT_CONNECTION_STRING_KEY = "connectionString";

  private String databaseKey;

  private String usernameKey;

  private String passwordKey;

  private String connectionStringKey;

  public String getDatabaseKey() {
    return databaseKey;
  }

  public SecretSpec setDatabaseKey(String databaseKey) {
    this.databaseKey = databaseKey;
    return this;
  }

  public String getUsernameKey() {
    return usernameKey;
  }

  public SecretSpec setUsernameKey(String usernameKey) {
    this.usernameKey = usernameKey;
    return this;
  }

  public String getPasswordKey() {
    return passwordKey;
  }

  public SecretSpec setPasswordKey(String passwordKey) {
    this.passwordKey = passwordKey;
    return this;
  }

  public String getConnectionStringKey() {
    return connectionStringKey;
  }

  public SecretSpec setConnectionStringKey(String connectionStringKey) {
    this.connectionStringKey = connectionStringKey;
    return this;
  }
}
