package com.sdase.k8s.operator.mongodb.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unfortunately an environment for a real world test is hard to setup. Therefore this test just
 * helps to get the adapter covered. For validation of correct usage, the separate end to end test
 * must be considered. We may mock the Kubernetes API later as in MongoDbOperatorTest. For now we
 * rely on correct implementation of the mocked KubernetesClient.
 */
@ExtendWith({MockitoExtension.class})
class KubernetesClientAdapterTest {

  @Mock KubernetesClient kubernetesClientMock;

  @Mock MixedOperation<Secret, SecretList, Resource<Secret>> secretMixedOperationMock;

  @Mock NonNamespaceOperation<Secret, SecretList, Resource<Secret>> secretNonNamespaceOperationMock;

  @InjectMocks KubernetesClientAdapter kubernetesClientAdapter;

  ArgumentCaptor<String> namespaceCaptor = ArgumentCaptor.forClass(String.class);
  ArgumentCaptor<Secret> createdSecretCaptor = ArgumentCaptor.forClass(Secret.class);

  @BeforeEach
  void setUp() {
    when(kubernetesClientMock.secrets()).thenReturn(secretMixedOperationMock);
    when(secretMixedOperationMock.inNamespace(namespaceCaptor.capture()))
        .thenReturn(secretNonNamespaceOperationMock);
  }

  @Test
  void shouldCallKubernetesClientToCreateSecret() {
    when(secretNonNamespaceOperationMock.create(createdSecretCaptor.capture()))
        .then(invocation -> invocation.getArgument(0));

    var givenSecret = new Secret();

    kubernetesClientAdapter.createSecretInNamespace("the-namespace", givenSecret);

    assertThat(namespaceCaptor.getAllValues()).containsExactly("the-namespace");
    assertThat(createdSecretCaptor.getAllValues()).containsExactly(givenSecret);
  }

  @Test
  void shouldPassThroughKubernetesClientExceptionWhenCreateSecretFails() {
    var givenException = new KubernetesClientException("Error");
    when(secretNonNamespaceOperationMock.create(createdSecretCaptor.capture()))
        .thenThrow(givenException);

    var givenSecret = new Secret();

    assertThatExceptionOfType(KubernetesClientException.class)
        .isThrownBy(
            () -> kubernetesClientAdapter.createSecretInNamespace("the-namespace", givenSecret))
        .isSameAs(givenException);

    assertThat(namespaceCaptor.getAllValues()).containsExactly("the-namespace");
    assertThat(createdSecretCaptor.getAllValues()).containsExactly(givenSecret);
  }
}
