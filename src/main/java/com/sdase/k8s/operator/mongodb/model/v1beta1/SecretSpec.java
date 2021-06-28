package com.sdase.k8s.operator.mongodb.model.v1beta1;

public class SecretSpec {

  private static final String DEFAULT_DATABASE_KEY = "database";
  private static final String DEFAULT_USERNAME_KEY = "username";
  private static final String DEFAULT_PASSWORD_KEY = "password";

  private String databaseKey = DEFAULT_DATABASE_KEY;

  private String usernameKey = DEFAULT_USERNAME_KEY;

  private String passwordKey = DEFAULT_PASSWORD_KEY;

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
}
