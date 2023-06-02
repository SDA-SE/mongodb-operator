package com.sdase.k8s.operator.mongodb.controller.tasks;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;

public record DeleteDatabaseTask(
    MongoDbCustomResource source, String databaseName, String username, boolean pruneDb) {}
