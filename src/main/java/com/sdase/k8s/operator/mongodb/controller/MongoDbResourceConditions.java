package com.sdase.k8s.operator.mongodb.controller;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbStatus;
import io.fabric8.kubernetes.api.model.Condition;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDbResourceConditions {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbResourceConditions.class);

  private static final String STATUS_TRUE = "True";
  private static final String STATUS_FALSE = "False";
  private static final String STATUS_UNKNOWN = "Unknown";
  private static final int EXPECTED_CONDITIONS_COUNT = 3;
  // will be multiplied by the number of attempts
  private static final Long RECONCILE_REQUEST_MIN_DURATION_ON_ERROR_SECONDS = 5L;
  private static final String DEFAULT_REASON = "CreateOrUpdate";

  private Boolean createUsernameSuccessful;
  private String usernameMessage = "";
  private Boolean createDatabaseSuccessful;
  private String createDatabaseMessage = "";
  private Boolean createSecretSuccessful;
  private String createSecretMessage = "";

  @SuppressWarnings("UnusedReturnValue")
  MongoDbResourceConditions applyUsernameCreated(String username) {
    this.createUsernameSuccessful = true;
    this.usernameMessage = "Username " + username + " created.";
    return this;
  }

  MongoDbResourceConditions applyUsernameCreationFailed(String usernameFailedErrorMessage) {
    this.createUsernameSuccessful = false;
    this.usernameMessage = usernameFailedErrorMessage;
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  MongoDbResourceConditions applyDatabaseCreated() {
    this.createDatabaseSuccessful = true;
    this.createDatabaseMessage = "Database created";
    return this;
  }

  MongoDbResourceConditions applyDatabaseCreationFailed() {
    this.createDatabaseSuccessful = false;
    this.createDatabaseMessage = "Database creation failed";
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  MongoDbResourceConditions applySecretCreated() {
    this.createSecretSuccessful = true;
    this.createSecretMessage = "Secret created";
    return this;
  }

  MongoDbResourceConditions applySecretCreationFailed(String message) {
    this.createSecretSuccessful = false;
    this.createSecretMessage = message;
    return this;
  }

  /**
   * @param original the resource that is handled
   * @return an {@code UpdateControl} with the <strong>modified</strong> original resource.
   */
  UpdateControl<MongoDbCustomResource> createStatusUpdate(MongoDbCustomResource original) {
    var resourceGeneration = findResourceGeneration(original);
    var resourceWithConditions =
        createResourceWithConditions(
            original,
            createCondition(
                resourceGeneration, createUsernameSuccessful, "CreateUsername", usernameMessage),
            createCondition(
                resourceGeneration,
                createDatabaseSuccessful,
                "CreateDatabase",
                createDatabaseMessage),
            createCondition(
                resourceGeneration, createSecretSuccessful, "CreateSecret", createSecretMessage));
    LOG.info(
        "Setting status of MongoDB {}/{}: username={}, database={}, secret={}, attempt={}",
        original.getMetadata().getNamespace(),
        original.getMetadata().getName(),
        createUsernameSuccessful,
        createDatabaseSuccessful,
        createSecretSuccessful,
        resourceWithConditions.getStatus().getAttempts());
    if (allConditionsTrue(resourceWithConditions)) {
      return UpdateControl.updateStatus(resourceWithConditions);
    } else {
      return UpdateControl.updateStatus(resourceWithConditions)
          .rescheduleAfter(
              RECONCILE_REQUEST_MIN_DURATION_ON_ERROR_SECONDS
                  * resourceWithConditions.getStatus().getAttempts(),
              TimeUnit.SECONDS);
    }
  }

  private boolean allConditionsTrue(MongoDbCustomResource resourceWithConditions) {
    return resourceWithConditions.getStatus().getConditions().stream()
        .map(Condition::getStatus)
        .allMatch(STATUS_TRUE::equals);
  }

  private Long findResourceGeneration(MongoDbCustomResource mongoDbCustomResource) {
    return mongoDbCustomResource.getMetadata().getGeneration();
  }

  private MongoDbCustomResource createResourceWithConditions(
      MongoDbCustomResource resource, Condition... conditions) {
    var attempts = 1L;
    if (resource.getStatus() != null) {
      attempts = resource.getStatus().getAttempts() + 1;
    }
    var status = new MongoDbStatus().setConditions(List.of(conditions)).setAttempts(attempts);
    resource.setStatus(status);
    return resource;
  }

  private Condition createCondition(
      Long generation, Boolean status, String conditionType, String conditionMessage) {
    var condition = new Condition();
    condition.setLastTransitionTime(Instant.now().toString());
    condition.setStatus(statusOf(status));
    condition.setType(conditionType);
    condition.setMessage(conditionMessage);
    condition.setReason(DEFAULT_REASON);
    condition.setObservedGeneration(generation);
    return condition;
  }

  private String statusOf(Boolean status) {
    if (status == null) {
      return STATUS_UNKNOWN;
    }
    return status ? STATUS_TRUE : STATUS_FALSE;
  }

  public boolean fulfilled(MongoDbCustomResource resource) {
    if (resource.getStatus() == null) {
      return false;
    }
    var conditions = resource.getStatus().getConditions();
    if (conditions == null) {
      return false;
    }
    return conditions.stream()
            .filter(
                c ->
                    Objects.equals(
                        resource.getMetadata().getGeneration(), c.getObservedGeneration()))
            .filter(c -> STATUS_TRUE.equals(c.getStatus()))
            .count()
        == EXPECTED_CONDITIONS_COUNT;
  }

  public boolean hasEmptyStatus(MongoDbCustomResource resource) {
    return Optional.ofNullable(resource)
        .map(MongoDbCustomResource::getStatus)
        .map(MongoDbStatus::getConditions)
        .map(List::isEmpty)
        .orElse(true);
  }
}
