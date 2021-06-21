package com.sdase.k8s.operator.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.RetryInfo;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MongoDbControllerTest {

  // FIXME this test is just to have the controller somehow covered.
  // We will most likely change what is happening in Controller when connecting Controller and
  // MongoDB. Then we will need to find a way to implement some integration tests as well or at
  // least have reasonable knowledge about the things we need to mock.

  KubernetesClientAdapter kubernetesClientAdapter = mock(KubernetesClientAdapter.class);

  MongoDbController mongoDbController =
      new MongoDbController(
          kubernetesClientAdapter, new V1SecretBuilder(() -> "static-test-password"));

  @Test
  void shouldPerformDefaultDelete() {

    var actual =
        mongoDbController.deleteResource(
            new MongoDbCustomResource(), new MongoDbCustomResourceContext());

    // TODO need to check if database is cleaned up as well

    // By default owned resources (like the created secret) will be deleted as well.
    assertThat(actual).isEqualTo(DeleteControl.DEFAULT_DELETE);
  }

  @Test
  void shouldNotChangeMongoDbResources() {

    var actual =
        mongoDbController.createOrUpdateResource(
            new MongoDbCustomResource(), new MongoDbCustomResourceContext());

    // TODO need to check if database, database user and secret is created

    // For the moment no updates to the original resource, this may change in the future if needed.
    assertThat(actual.isUpdateCustomResource()).isFalse();
    assertThat(actual.isUpdateStatusSubResource()).isFalse();
    assertThat(actual.isUpdateCustomResourceAndStatusSubResource()).isFalse();
  }

  private static class MongoDbCustomResourceContext implements Context<MongoDbCustomResource> {

    @Override
    public EventList getEvents() {
      return new EventList(List.of());
    }

    @Override
    public Optional<RetryInfo> getRetryInfo() {
      return Optional.empty();
    }
  }
}
