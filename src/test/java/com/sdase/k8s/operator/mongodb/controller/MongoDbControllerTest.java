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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.controller.tasks.util.NamingUtil;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbSpec;
import com.sdase.k8s.operator.mongodb.model.v1beta1.SecretSpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.RetryInfo;
import io.javaoperatorsdk.operator.processing.event.EventList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class MongoDbControllerTest {

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

    var actual =
        mongoDbController.deleteResource(
            new MongoDbCustomResource(), new MongoDbCustomResourceContext());

    // TODO HPC-898 need to check if database is cleaned up as well

    // By default owned resources (like the created secret) will be deleted as well.
    assertThat(actual).isEqualTo(DeleteControl.DEFAULT_DELETE);
  }

  @Test
  void shouldCallAllNecessaryServicesForSuccess() {
    var secretArgumentCaptor = ArgumentCaptor.forClass(Secret.class);
    doNothing()
        .when(kubernetesClientAdapterMock)
        .createSecretInNamespace(anyString(), secretArgumentCaptor.capture());
    when(mongoDbServiceMock.createDatabaseWithUser(anyString(), anyString(), anyString()))
        .thenReturn(true);

    var givenMongoDbCr = new MongoDbCustomResource();
    givenMongoDbCr.setMetadata(
        new ObjectMetaBuilder().withNamespace("the-namespace").withName("the-name").build());
    givenMongoDbCr.setSpec(
        new MongoDbSpec().setSecret(new SecretSpec().setUsernameKey("u").setPasswordKey("p")));

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
              .extracting("u", "p")
              .containsExactly("dGhlLW5hbWVzcGFjZV90aGUtbmFtZQ==", "c3RhdGljLXRlc3QtcGFzc3dvcmQ=");

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
    verifyZeroInteractions(kubernetesClientAdapterMock);
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
