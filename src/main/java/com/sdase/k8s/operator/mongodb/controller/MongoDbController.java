package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.controller.tasks.CreateDatabaseTask;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.IllegalNameException;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService.CreateDatabaseResult;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerConfiguration
public class MongoDbController
    implements Reconciler<MongoDbCustomResource>, Cleaner<MongoDbCustomResource> {

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
  public DeleteControl cleanup(
      MongoDbCustomResource resource, Context<MongoDbCustomResource> context) {
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
          "Ignoring delete request for MongoDb {}/{}, name is invalid. The database may not exist. Reason: {}",
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName(),
          e.getMessage());
    }
    return DeleteControl.defaultDelete();
  }

  @Override
  public UpdateControl<MongoDbCustomResource> reconcile(
      MongoDbCustomResource resource, Context<MongoDbCustomResource> context) {
    LOG.info(
        "MongoDb {}/{} created or updated",
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    var conditions = new MongoDbResourceConditions();
    if (conditions.fulfilled(resource)) {
      LOG.info(
          "MongoDb {}/{} already up to date",
          resource.getMetadata().getNamespace(),
          resource.getMetadata().getName());
      return UpdateControl.noUpdate();
    }

    createDatabaseTask(resource, conditions)
        .filter(task -> CreateDatabaseResult.CREATED == createDatabase(task, resource, conditions))
        .ifPresent(task -> createSecret(task, resource, conditions));

    return conditions.createStatusUpdate(resource);
  }

  private Optional<CreateDatabaseTask> createDatabaseTask(
      MongoDbCustomResource resource, MongoDbResourceConditions trackedConditions) {
    try {
      var task = taskFactory.newCreateTask(resource, mongoDbService.getConnectionString());
      trackedConditions.applyUsernameCreated(task.getUsername());
      return Optional.of(task);
    } catch (IllegalNameException e) {
      trackedConditions.applyUsernameCreationFailed(e.getMessage());
      return Optional.empty();
    }
  }

  private CreateDatabaseResult createDatabase(
      CreateDatabaseTask task,
      MongoDbCustomResource resource,
      MongoDbResourceConditions trackedConditions) {
    var databaseCreated =
        mongoDbService.createDatabaseWithUser(
            task.getDatabaseName(), task.getUsername(), task.getPassword());
    if (databaseCreated == CreateDatabaseResult.FAILED) {
      trackedConditions.applyDatabaseCreationFailed();
      return databaseCreated;
    }
    if (databaseCreated == CreateDatabaseResult.SKIPPED
        && trackedConditions.hasEmptyStatus(resource)) {
      // If database/user already exists and there is no status in the resource, we assume that
      // the resource has been handled by an older version, where no status was tracked.
      // We have to assume, that the secret has been successfully created, because the MongoDB
      // Operator has no read access to secrets by default.
      trackedConditions.applyDatabaseCreated().applySecretCreated();
      return databaseCreated;
    }
    trackedConditions.applyDatabaseCreated();
    return databaseCreated;
  }

  private void createSecret(
      CreateDatabaseTask task,
      MongoDbCustomResource resource,
      MongoDbResourceConditions trackedConditions) {
    try {
      var secret = v1SecretBuilder.createSecretForOwner(task);
      kubernetesClientAdapter.createSecretInNamespace(
          resource.getMetadata().getNamespace(), secret);
      trackedConditions.applySecretCreated();
    } catch (KubernetesClientException e) {
      trackedConditions.applySecretCreationFailed(e.getMessage()).createStatusUpdate(resource);
    }
  }
}
