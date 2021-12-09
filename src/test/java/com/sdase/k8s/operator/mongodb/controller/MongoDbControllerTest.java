package com.sdase.k8s.operator.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.ConnectionString;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.NamingUtil;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.model.v1beta1.DatabaseSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.SecretSpec;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.RetryInfo;
import io.javaoperatorsdk.operator.processing.event.EventList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class MongoDbControllerTest {

  private static final ConnectionString MONGODB_OPERATOR_CONNECTION_STRING =
      new ConnectionString(
          "mongodb://"
              + "mongodb-operator:suer-s3cr35"
              + "@some-documentdb.c123456.eu-central-1.docdb.amazonaws.com:27017"
              + ",some-documentdb.c789012.eu-central-1.docdb.amazonaws.com:27017"
              + "/admin");

  @Mock KubernetesClientAdapter kubernetesClientAdapterMock;

  @Spy
  TaskFactory taskFactorySpy =
      TaskFactory.customFactory(
          NamingUtil::fromNamespaceAndName,
          mdbCr -> "static-test-password",
          NamingUtil::fromNamespaceAndName);

  @Mock MongoDbService mongoDbServiceMock;

  @Spy V1SecretBuilder v1SecretBuilderMock = new V1SecretBuilder();

  @InjectMocks MongoDbController mongoDbController;

  @Test
  void shouldPerformDefaultDelete() {
    when(mongoDbServiceMock.dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name"))
        .thenReturn(true);

    var givenMetadata = new ObjectMeta();
    givenMetadata.setNamespace("the-namespace");
    givenMetadata.setName("the-name");
    var given = new MongoDbCustomResource();
    given.setMetadata(givenMetadata);

    var actual = mongoDbController.deleteResource(given, new MongoDbCustomResourceContext());

    // By default owned resources (like the created secret) will be deleted as well.
    assertThat(actual).isEqualTo(DeleteControl.DEFAULT_DELETE);
    verify(mongoDbServiceMock).dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name");
    verifyNoMoreInteractions(mongoDbServiceMock);
  }

  @Test
  void shouldSkipDeleteDatabaseAfterLastAttempt() {
    when(mongoDbServiceMock.dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name"))
        .thenReturn(true);
    when(mongoDbServiceMock.dropDatabase("the-namespace_the-name")).thenReturn(false);

    var givenMetadata = new ObjectMeta();
    givenMetadata.setNamespace("the-namespace");
    givenMetadata.setName("the-name");
    var given = new MongoDbCustomResource();
    given.setMetadata(givenMetadata);
    given.getSpec().getDatabase().setPruneAfterDelete(true);

    var retryInfoMock = mock(RetryInfo.class);
    when(retryInfoMock.isLastAttempt()).thenReturn(true);
    var contextMock = mock(MongoDbCustomResourceContext.class);
    when(contextMock.getRetryInfo()).thenReturn(Optional.of(retryInfoMock));

    var actual = mongoDbController.deleteResource(given, contextMock);

    // By default owned resources (like the created secret) will be deleted as well.
    assertThat(actual).isEqualTo(DeleteControl.DEFAULT_DELETE);
    verify(mongoDbServiceMock).dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name");
    verifyNoMoreInteractions(mongoDbServiceMock);
  }

  @Test
  void shouldFailWhenUserCantBeDeleted() {
    when(mongoDbServiceMock.dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name"))
        .thenReturn(false);

    var givenMetadata = new ObjectMeta();
    givenMetadata.setNamespace("the-namespace");
    givenMetadata.setName("the-name");
    var given = new MongoDbCustomResource();
    given.setMetadata(givenMetadata);

    var givenContext = new MongoDbCustomResourceContext();

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> mongoDbController.deleteResource(given, givenContext));

    // By default owned resources (like the created secret) will be deleted as well.
    verify(mongoDbServiceMock).dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name");
  }

  @Test
  void shouldPerformDeleteWithPruneDb() {
    when(mongoDbServiceMock.dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name"))
        .thenReturn(true);
    when(mongoDbServiceMock.dropDatabase("the-namespace_the-name")).thenReturn(true);

    var givenMetadata = new ObjectMeta();
    givenMetadata.setNamespace("the-namespace");
    givenMetadata.setName("the-name");
    var given = new MongoDbCustomResource();
    given.setMetadata(givenMetadata);
    given.getSpec().getDatabase().setPruneAfterDelete(true);

    var actual = mongoDbController.deleteResource(given, new MongoDbCustomResourceContext());

    // By default owned resources (like the created secret) will be deleted as well.
    assertThat(actual).isEqualTo(DeleteControl.DEFAULT_DELETE);
    verify(mongoDbServiceMock).dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name");
    verify(mongoDbServiceMock).dropDatabase("the-namespace_the-name");
  }

  @Test
  void shouldShouldFailWhenPruneDbNotPossible() {
    when(mongoDbServiceMock.dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name"))
        .thenReturn(true);
    when(mongoDbServiceMock.dropDatabase("the-namespace_the-name")).thenReturn(false);

    var givenMetadata = new ObjectMeta();
    givenMetadata.setNamespace("the-namespace");
    givenMetadata.setName("the-name");
    var given = new MongoDbCustomResource();
    given.setMetadata(givenMetadata);
    given.getSpec().getDatabase().setPruneAfterDelete(true);

    var retryInfoMock = mock(RetryInfo.class);
    when(retryInfoMock.isLastAttempt()).thenReturn(false);
    var contextMock = mock(MongoDbCustomResourceContext.class);
    when(contextMock.getRetryInfo()).thenReturn(Optional.of(retryInfoMock));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> mongoDbController.deleteResource(given, contextMock));

    // By default owned resources (like the created secret) will be deleted as well.
    verify(mongoDbServiceMock).dropDatabaseUser("the-namespace_the-name", "the-namespace_the-name");
    verify(mongoDbServiceMock).dropDatabase("the-namespace_the-name");
  }

  @Test
  void shouldAllowDeleteIfDatabaseNameIsInvalid() {
    var givenMetadata = new ObjectMeta();
    givenMetadata.setNamespace("the-namespace" + StringUtils.repeat("-123456789", 3));
    givenMetadata.setName("the-name" + StringUtils.repeat("-123456789", 3));
    var given = new MongoDbCustomResource();
    given.setMetadata(givenMetadata);
    given.getSpec().getDatabase().setPruneAfterDelete(true);

    var givenContext = new MongoDbCustomResourceContext();

    var actual = mongoDbController.deleteResource(given, givenContext);

    assertThat(actual).isEqualTo(DeleteControl.DEFAULT_DELETE);

    verifyNoInteractions(mongoDbServiceMock);
    verifyNoInteractions(mongoDbServiceMock);
  }

  @Test
  void shouldCallAllNecessaryServicesForSuccess() {
    var secretArgumentCaptor = ArgumentCaptor.forClass(Secret.class);
    doNothing()
        .when(kubernetesClientAdapterMock)
        .createSecretInNamespace(anyString(), secretArgumentCaptor.capture());
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec()
            .setSecret(
                new SecretSpec()
                    .setUsernameKey("u")
                    .setPasswordKey("p")
                    .setConnectionStringKey("c"))
            .setDatabase(
                new DatabaseSpec()
                    .setConnectionStringOptions(
                        "tls=true&readPreference=secondaryPreferred&retryWrites=false")));

    var actual =
        mongoDbController.createOrUpdateResource(
            givenMongoDbCr, new MongoDbCustomResourceContext());

    verify(mongoDbServiceMock, times(1))
        .createDatabaseWithUser(
            "the-namespace_the-name", "the-namespace_the-name", "static-test-password");
    verify(kubernetesClientAdapterMock, times(1))
        .createSecretInNamespace(eq("the-namespace"), any(Secret.class));

    assertSoftly(
        softly -> {
          softly
              .assertThat(secretArgumentCaptor.getValue())
              .extracting(Secret::getMetadata)
              .extracting("namespace", "name")
              .containsExactly("the-namespace", "the-name");
          softly
              .assertThat(secretArgumentCaptor.getValue())
              .extracting(Secret::getData)
              .extracting("u", "p", "c")
              .containsExactly(
                  "dGhlLW5hbWVzcGFjZV90aGUtbmFtZQ==",
                  "c3RhdGljLXRlc3QtcGFzc3dvcmQ=",
                  "bW9uZ29kYjovL3RoZS1uYW1lc3BhY2VfdGhlLW5hbWU6c3RhdGljLXRlc3QtcGFzc3dvcmRAc29tZS1kb2N1bWVudGRiLmMxMjM0NTYuZXUtY2VudHJhbC0xLmRvY2RiLmFtYXpvbmF3cy5jb206MjcwMTcsc29tZS1kb2N1bWVudGRiLmM3ODkwMTIuZXUtY2VudHJhbC0xLmRvY2RiLmFtYXpvbmF3cy5jb206MjcwMTcvdGhlLW5hbWVzcGFjZV90aGUtbmFtZT90bHM9dHJ1ZSZyZWFkUHJlZmVyZW5jZT1zZWNvbmRhcnlQcmVmZXJyZWQmcmV0cnlXcml0ZXM9ZmFsc2U=");

          // For the moment no updates to the original resource, this may change in the future if
          // needed.
          softly.assertThat(actual.isUpdateCustomResource()).isFalse();
          softly.assertThat(actual.isUpdateStatusSubResource()).isFalse();
          softly.assertThat(actual.isUpdateCustomResourceAndStatusSubResource()).isFalse();
        });
  }

  @Test
  void shouldCallAllNecessaryServicesForSuccessForDocumentDb() {
    var secretArgumentCaptor = ArgumentCaptor.forClass(Secret.class);
    doNothing()
        .when(kubernetesClientAdapterMock)
        .createSecretInNamespace(anyString(), secretArgumentCaptor.capture());
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec()
            .setSecret(
                new SecretSpec()
                    .setUsernameKey("u")
                    .setPasswordKey("p")
                    .setConnectionStringKey("c"))
            .setDatabase(
                new DatabaseSpec()
                    .setConnectionStringOptions(
                        "tls=true&readPreference=secondaryPreferred&retryWrites=false")));

    var actual =
        mongoDbController.createOrUpdateResource(
            givenMongoDbCr, new MongoDbCustomResourceContext());

    verify(mongoDbServiceMock, times(1))
        .createDatabaseWithUser(
            "the-namespace_the-name", "the-namespace_the-name", "static-test-password");
    verify(kubernetesClientAdapterMock, times(1))
        .createSecretInNamespace(eq("the-namespace"), any(Secret.class));

    assertSoftly(
        softly -> {
          softly
              .assertThat(secretArgumentCaptor.getValue())
              .extracting(Secret::getMetadata)
              .extracting("namespace", "name")
              .containsExactly("the-namespace", "the-name");
          softly
              .assertThat(secretArgumentCaptor.getValue())
              .extracting(Secret::getData)
              .extracting("u", "p", "c")
              .containsExactly(
                  "dGhlLW5hbWVzcGFjZV90aGUtbmFtZQ==",
                  "c3RhdGljLXRlc3QtcGFzc3dvcmQ=",
                  "bW9uZ29kYjovL3RoZS1uYW1lc3BhY2VfdGhlLW5hbWU6c3RhdGljLXRlc3QtcGFzc3dvcmRAc29tZS1kb2N1bWVudGRiLmMxMjM0NTYuZXUtY2VudHJhbC0xLmRvY2RiLmFtYXpvbmF3cy5jb206MjcwMTcsc29tZS1kb2N1bWVudGRiLmM3ODkwMTIuZXUtY2VudHJhbC0xLmRvY2RiLmFtYXpvbmF3cy5jb206MjcwMTcvdGhlLW5hbWVzcGFjZV90aGUtbmFtZT90bHM9dHJ1ZSZyZWFkUHJlZmVyZW5jZT1zZWNvbmRhcnlQcmVmZXJyZWQmcmV0cnlXcml0ZXM9ZmFsc2U=");

          // For the moment no updates to the original resource, this may change in the future if
          // needed.
          softly.assertThat(actual.isUpdateCustomResource()).isFalse();
          softly.assertThat(actual.isUpdateStatusSubResource()).isFalse();
          softly.assertThat(actual.isUpdateCustomResourceAndStatusSubResource()).isFalse();
        });
  }

  @Test
  void shouldFailWhenSecretCantBeCreated() {
    var secretArgumentCaptor = ArgumentCaptor.forClass(Secret.class);
    var givenException = new KubernetesClientException("Error");
    doThrow(givenException)
        .when(kubernetesClientAdapterMock)
        .createSecretInNamespace(anyString(), secretArgumentCaptor.capture());
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));
    var givenContext = new MongoDbCustomResourceContext();

    assertThatExceptionOfType(KubernetesClientException.class)
        .isThrownBy(() -> mongoDbController.createOrUpdateResource(givenMongoDbCr, givenContext))
        .isSameAs(givenException);

    verify(mongoDbServiceMock, times(1))
        .createDatabaseWithUser(
            "the-namespace_the-name", "the-namespace_the-name", "static-test-password");
    verify(kubernetesClientAdapterMock, times(1))
        .createSecretInNamespace(eq("the-namespace"), any(Secret.class));

    assertSoftly(
        softly -> {
          softly
              .assertThat(secretArgumentCaptor.getValue())
              .extracting(Secret::getMetadata)
              .extracting("namespace", "name")
              .containsExactly("the-namespace", "the-name");
          softly
              .assertThat(secretArgumentCaptor.getValue())
              .extracting(Secret::getData)
              .extracting("u", "p")
              .containsExactly("dGhlLW5hbWVzcGFjZV90aGUtbmFtZQ==", "c3RhdGljLXRlc3QtcGFzc3dvcmQ=");
        });
  }

  @Test
  void shouldNotCreateSecretButFailWhenNoDatabaseHasBeenCreated() {
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(false);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));
    var givenContext = new MongoDbCustomResourceContext();

    assertThatIllegalStateException()
        .isThrownBy(() -> mongoDbController.createOrUpdateResource(givenMongoDbCr, givenContext));

    verify(mongoDbServiceMock, times(1))
        .createDatabaseWithUser(
            "the-namespace_the-name", "the-namespace_the-name", "static-test-password");
    verifyNoInteractions(kubernetesClientAdapterMock);
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
