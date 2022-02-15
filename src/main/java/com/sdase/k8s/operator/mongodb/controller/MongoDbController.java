package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.IllegalNameException;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerConfiguration
public class MongoDbController implements Reconciler<MongoDbCustomResource> {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbController.class);

  private final KubernetesClientAdapter kubernetesClientAdapter;
  private final TaskFactory taskFactory;
  private final MongoDbService mongoDbService;
  private final V1SecretBuilder v1SecretBuilder;

  public MongoDbController(
      KubernetesClientAdapter kubernetesClientAdapter,
      TaskFactory taskFactory,
      MongoDbService mongoDbService,
      V1SecretBuilder v1SecretBuilder) {
    this.kubernetesClientAdapter = kubernetesClientAdapter;
    this.taskFactory = taskFactory;
    this.mongoDbService = mongoDbService;
    this.v1SecretBuilder = v1SecretBuilder;
  }

  @Override
  public DeleteControl cleanup(MongoDbCustomResource resource, Context context) {
    LOG.info(
        "MongoDb {}/{} deleted",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    try {
      var deleteDatabaseTask = taskFactory.newDeleteTask(resource);
      var userDropped =
          mongoDbService.dropDatabaseUser(
              deleteDatabaseTask.getDatabaseName(), deleteDatabaseTask.getUsername());
      if (!userDropped) {
        throw new IllegalStateException("Failed to drop user");
      }
      if (deleteDatabaseTask.isPruneDb()) {
        boolean databaseDeleted = mongoDbService.dropDatabase(deleteDatabaseTask.getDatabaseName());
        if (!databaseDeleted) {
          if (context.getRetryInfo().map(RetryInfo::isLastAttempt).orElse(false)) {
            LOG.warn(
                "Last attempt to delete database {} failed. Skipping.",
                deleteDatabaseTask.getDatabaseName());
            return DeleteControl.defaultDelete();
          }
          throw new IllegalStateException("Failed to drop database");
        }
      }
    } catch (IllegalNameException e) {
      LOG.warn(
          "Ignoring delete request for MongoDb {}/{}, name is invalid. The database may not exist.",
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName(),
          e);
    }
    return DeleteControl.defaultDelete();
  }

  @Override
  public UpdateControl<MongoDbCustomResource> reconcile(
      MongoDbCustomResource resource, Context context) {
    LOG.info(
        "MongoDb {}/{} created or updated",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    var task = taskFactory.newCreateTask(resource, mongoDbService.getConnectionString());
    var secret = v1SecretBuilder.createSecretForOwner(task);
    var databaseCreated =
        mongoDbService.createDatabaseWithUser(
            task.getDatabaseName(), task.getUsername(), task.getPassword());
    if (!databaseCreated) {
      // maybe update status in the future but probably requires write access to the MongoDB CR
      throw new IllegalStateException("Failed to create database");
    }
    kubernetesClientAdapter.createSecretInNamespace(resource.getMetadata().getNamespace(), secret);
    return UpdateControl.noUpdate();
  }
}
