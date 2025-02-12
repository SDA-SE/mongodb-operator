package com.sdase.k8s.operator.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService.CreateDatabaseResult;
import com.sdase.k8s.operator.mongodb.model.v1beta1.DatabaseSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbStatus;
import com.sdase.k8s.operator.mongodb.model.v1beta1.SecretSpec;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.IndexedResourceCache;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.managed.ManagedWorkflowAndDependentResourceContext;
import io.javaoperatorsdk.operator.processing.event.EventSourceRetriever;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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

  @SuppressWarnings("unused")
  @Spy
  V1SecretBuilder v1SecretBuilderMock = new V1SecretBuilder();

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

    var actual = mongoDbController.cleanup(given, new MongoDbCustomResourceContext());

    // By default, owned resources (like the created secret) will be deleted as well.
    assertThat(actual).usingRecursiveComparison().isEqualTo(DeleteControl.defaultDelete());
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

    var actual = mongoDbController.cleanup(given, contextMock);

    // By default, owned resources (like the created secret) will be deleted as well.
    assertThat(actual).usingRecursiveComparison().isEqualTo(DeleteControl.defaultDelete());
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
        .isThrownBy(() -> mongoDbController.cleanup(given, givenContext));

    // By default, owned resources (like the created secret) will be deleted as well.
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

    var actual = mongoDbController.cleanup(given, new MongoDbCustomResourceContext());

    // By default, owned resources (like the created secret) will be deleted as well.
    assertThat(actual).usingRecursiveComparison().isEqualTo(DeleteControl.defaultDelete());
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
        .isThrownBy(() -> mongoDbController.cleanup(given, contextMock));

    // By default, owned resources (like the created secret) will be deleted as well.
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

    var actual = mongoDbController.cleanup(given, givenContext);

    assertThat(actual).usingRecursiveComparison().isEqualTo(DeleteControl.defaultDelete());

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
        .thenReturn(CreateDatabaseResult.CREATED);
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

    var actual = mongoDbController.reconcile(givenMongoDbCr, new MongoDbCustomResourceContext());

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
          softly.assertThat(actual.isPatchResource()).isFalse();
          softly.assertThat(actual.isPatchStatus()).isTrue();
          softly.assertThat(areAllStatusConditionsTrue(actual)).isTrue();
          softly.assertThat(actual.isPatchResourceAndStatus()).isFalse();
          softly.assertThat(actual.isNoUpdate()).isFalse();
        });
  }

  @Test
  void shouldFailWithBadConditionsForTooLongName() {
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder()
            .withNamespace("the-namespace")
            .withName("the-database-with-way-too-long-name-that-exceeds-all-limits")
            .build());
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

    var actual = mongoDbController.reconcile(givenMongoDbCr, new MongoDbCustomResourceContext());

    assertSoftly(
        softly -> {
          // For the moment no updates to the original resource, this may change in the future if
          // needed.
          softly.assertThat(actual.isPatchResource()).isFalse();
          softly.assertThat(actual.isPatchStatus()).isTrue();
          softly
              .assertThat(extractStatusConditions(actual))
              .containsEntry("CreateUsername", "False")
              .containsEntry("CreateDatabase", "Unknown")
              .containsEntry("CreateSecret", "Unknown");
          softly.assertThat(actual.isPatchResourceAndStatus()).isFalse();
          softly.assertThat(actual.isNoUpdate()).isFalse();
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
        .thenReturn(CreateDatabaseResult.CREATED);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));
    var givenContext = new MongoDbCustomResourceContext();

    var actual = mongoDbController.reconcile(givenMongoDbCr, givenContext);

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
          softly.assertThat(actual.isPatchResource()).isFalse();
          softly.assertThat(actual.isPatchStatus()).isTrue();
          softly
              .assertThat(extractStatusConditions(actual))
              .containsEntry("CreateUsername", "True")
              .containsEntry("CreateDatabase", "True")
              .containsEntry("CreateSecret", "False");
          softly.assertThat(actual.isPatchResourceAndStatus()).isFalse();
          softly.assertThat(actual.isNoUpdate()).isFalse();
        });
  }

  @Test
  void shouldNotCreateSecretButFailWhenNoDatabaseHasBeenCreated() {
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(CreateDatabaseResult.FAILED);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));
    var givenContext = new MongoDbCustomResourceContext();

    var actual = mongoDbController.reconcile(givenMongoDbCr, givenContext);

    verify(mongoDbServiceMock, times(1))
        .createDatabaseWithUser(
            "the-namespace_the-name", "the-namespace_the-name", "static-test-password");
    verifyNoInteractions(kubernetesClientAdapterMock);
    assertSoftly(
        softly -> {
          softly.assertThat(actual.isPatchResource()).isFalse();
          softly.assertThat(actual.isPatchStatus()).isTrue();
          softly
              .assertThat(extractStatusConditions(actual))
              .containsEntry("CreateUsername", "True")
              .containsEntry("CreateDatabase", "False")
              .containsEntry("CreateSecret", "Unknown");
          softly.assertThat(actual.isPatchResourceAndStatus()).isFalse();
          softly.assertThat(actual.isNoUpdate()).isFalse();
        });
  }

  @Test
  void shouldUpdateStatusOfExistingResourcesWithoutStatus() {
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(CreateDatabaseResult.SKIPPED);
    when(mongoDbServiceMock.getConnectionString()).thenReturn(MONGODB_OPERATOR_CONNECTION_STRING);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));
    var givenContext = new MongoDbCustomResourceContext();

    var actual = mongoDbController.reconcile(givenMongoDbCr, givenContext);

    verify(mongoDbServiceMock, times(1))
        .createDatabaseWithUser(
            "the-namespace_the-name", "the-namespace_the-name", "static-test-password");
    verifyNoInteractions(kubernetesClientAdapterMock);
    assertSoftly(
        softly -> {
          softly.assertThat(actual.isPatchResource()).isFalse();
          softly.assertThat(actual.isPatchStatus()).isTrue();
          softly
              .assertThat(extractStatusConditions(actual))
              .containsEntry("CreateUsername", "True")
              .containsEntry("CreateDatabase", "True")
              .containsEntry("CreateSecret", "True");
          softly.assertThat(actual.isPatchResourceAndStatus()).isFalse();
          softly.assertThat(actual.isNoUpdate()).isFalse();
        });
  }

  @Test
  void shouldDoNothingWhenStatusIsComplete() {
    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));
    //noinspection OptionalGetWithoutIsPresent
    givenMongoDbCr =
        new MongoDbResourceConditions()
            .applyUsernameCreated("the-namespace_the-name")
            .applyDatabaseCreated()
            .applySecretCreated()
            .createStatusUpdate(givenMongoDbCr)
            .getResource()
            .get();
    var givenContext = new MongoDbCustomResourceContext();

    var actual = mongoDbController.reconcile(givenMongoDbCr, givenContext);

    assertSoftly(
        softly -> {
          softly.assertThat(actual.isPatchResource()).isFalse();
          softly.assertThat(actual.isPatchStatus()).isFalse();
          softly.assertThat(actual.isPatchResourceAndStatus()).isFalse();
          softly.assertThat(actual.isNoUpdate()).isTrue();
        });
    verifyNoInteractions(taskFactorySpy, mongoDbServiceMock, kubernetesClientAdapterMock);
  }

  private boolean areAllStatusConditionsTrue(UpdateControl<MongoDbCustomResource> resource) {
    Map<String, String> conditionsByName = extractStatusConditions(resource);
    return !conditionsByName.isEmpty()
        && conditionsByName.values().stream().allMatch("True"::equals);
  }

  private Map<String, String> extractStatusConditions(
      UpdateControl<MongoDbCustomResource> resource) {
    return resource
        .getResource()
        .map(MongoDbCustomResource::getStatus)
        .map(MongoDbStatus::getConditions)
        .map(
            conditions ->
                conditions.stream()
                    .collect(Collectors.toMap(Condition::getType, Condition::getStatus)))
        .orElse(new HashMap<>());
  }

  private class MongoDbCustomResourceContext implements Context<MongoDbCustomResource> {

    @Override
    public Optional<RetryInfo> getRetryInfo() {
      return Optional.empty();
    }

    @Override
    public <T> Set<T> getSecondaryResources(Class<T> expectedType) {
      return null;
    }

    @Override
    public <T> Optional<T> getSecondaryResource(Class<T> expectedType, String eventSourceName) {
      return Optional.empty();
    }

    @Override
    public ControllerConfiguration<MongoDbCustomResource> getControllerConfiguration() {
      return null;
    }

    @Override
    public ManagedWorkflowAndDependentResourceContext managedWorkflowAndDependentResourceContext() {
      return null;
    }

    @Override
    public EventSourceRetriever<MongoDbCustomResource> eventSourceRetriever() {
      return null;
    }

    @Override
    public KubernetesClient getClient() {
      return kubernetesClientAdapterMock.getKubernetesClient();
    }

    @Override
    public ExecutorService getWorkflowExecutorService() {
      return null;
    }

    @Override
    public IndexedResourceCache<MongoDbCustomResource> getPrimaryCache() {
      return null;
    }

    @Override
    public boolean isNextReconciliationImminent() {
      return false;
    }
  }
}
