package com.sdase.k8s.operator.mongodb.db.manager.model;

import java.util.ArrayList;
import java.util.List;

public class User {

  private final String id;

  private final String username;

  private final List<UserRole> roles;

  public User(String userId, String username, List<UserRole> roles) {
    this.id = userId;
    this.username = username;
    this.roles = roles != null ? new ArrayList<>(roles) : List.of();
  }

  public String getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public List<UserRole> getRoles() {
    return roles;
  }

  public static class UserRole {
    private String role;
    private String db;

    public UserRole(String role, String db) {
      this.role = role;
      this.db = db;
    }

    public String getRole() {
      return role;
    }

    public String getDb() {
      return db;
    }
  }
}
