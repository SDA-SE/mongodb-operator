package com.sdase.k8s.operator.mongodb.controller.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.sdase.k8s.operator.mongodb.controller.tasks.util.ConnectionStringUtil;
import com.sdase.k8s.operator.mongodb.model.v1beta1.DatabaseSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbSpec;
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
    given.setSpec(
        new MongoDbSpec()
            .setDatabase(
                new DatabaseSpec()
                    .setConnectionStringOptions(
                        "readPreference=secondaryPreferred&retryWrites=false")));

    var actual = defaultTaskFactory.newCreateTask(given, "mongodb0.example.com:27017");

    assertThat(actual.getSource()).isSameAs(given);
    assertThat(actual.getDatabaseName()).contains(givenNamespace).contains(givenName);
    assertThat(actual.getUsername()).contains(givenNamespace).contains(givenName);
    final var password = actual.getPassword();
    assertThat(password).isNotBlank();
    assertThat(actual.getConnectionString())
        .isEqualTo(
            ConnectionStringUtil.createConnectionString(
                actual.getDatabaseName(),
                actual.getUsername(),
                actual.getPassword(),
                "mongodb0.example.com:27017",
                "readPreference=secondaryPreferred&retryWrites=false"));
  }
}
