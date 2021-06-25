package com.sdase.k8s.operator.mongodb.controller.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.Test;

class TaskFactoryTest {

  TaskFactory defaultTaskFactory = TaskFactory.defaultFactory();

  @Test
  void shouldBuildDeleteTaskFromSource() {
    var givenNamespace = "my-namespace";
    var givenName = "my-name";

    var given = new MongoDbCustomResource();
    given.setMetadata(
        new ObjectMetaBuilder().withNamespace(givenNamespace).withName(givenName).build());

    var actual = defaultTaskFactory.newDeleteTask(given);

    assertThat(actual.getSource()).isSameAs(given);
    assertThat(actual.getDatabaseName()).contains(givenNamespace).contains(givenName);
    assertThat(actual.getUsername()).contains(givenNamespace).contains(givenName);
  }

  @Test
  void shouldBuildCreateTaskFromSource() {
    var givenNamespace = "my-namespace";
    var givenName = "my-name";

    var given = new MongoDbCustomResource();
    given.setMetadata(
        new ObjectMetaBuilder().withNamespace(givenNamespace).withName(givenName).build());

    var actual = defaultTaskFactory.newCreateTask(given);

    assertThat(actual.getSource()).isSameAs(given);
    assertThat(actual.getDatabaseName()).contains(givenNamespace).contains(givenName);
    assertThat(actual.getUsername()).contains(givenNamespace).contains(givenName);
    assertThat(actual.getPassword()).isNotBlank();
  }
}
