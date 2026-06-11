package com.sdase.k8s.operator.mongodb.controller;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Optional;

public class KubernetesClientAdapter {

  private final KubernetesClient kubernetesClient;

  public KubernetesClientAdapter(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  void createSecretInNamespace(String namespace, Secret secret) {
    kubernetesClient.secrets().inNamespace(namespace).resource(secret).create();
  }

  void createOrReplaceSecretInNamespace(String namespace, Secret secret) {
    kubernetesClient.secrets().inNamespace(namespace).resource(secret).createOrReplace();
  }

  Optional<Secret> getSecretInNamespace(String namespace, String name) {
    return Optional.ofNullable(
        kubernetesClient.secrets().inNamespace(namespace).withName(name).get());
  }

  KubernetesClient getKubernetesClient() {
    return kubernetesClient;
  }
}
