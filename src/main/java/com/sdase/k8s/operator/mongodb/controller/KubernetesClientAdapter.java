package com.sdase.k8s.operator.mongodb.controller;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesClientAdapter {

  private final KubernetesClient kubernetesClient;

  public KubernetesClientAdapter(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  void createSecretInNamespace(String namespace, Secret secret) {
    kubernetesClient.secrets().inNamespace(namespace).create(secret);
  }

  KubernetesClient getKubernetesClient() {
    return kubernetesClient;
  }
}
