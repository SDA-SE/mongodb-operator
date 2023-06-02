package com.sdase.k8s.operator.mongodb.controller.tasks;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;

public record CreateDatabaseTask(
    MongoDbCustomResource source,
    String databaseName,
    String username,
    String password,
    String connectionString) {}
