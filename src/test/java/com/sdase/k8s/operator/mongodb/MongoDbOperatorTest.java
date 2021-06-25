package com.sdase.k8s.operator.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sdase.k8s.operator.mongodb.db.manager.AbstractMongoDbTest;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServerExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith({SystemStubsExtension.class, KubernetesMockServerExtension.class})
@EnableKubernetesMockClient
class MongoDbOperatorTest extends AbstractMongoDbTest {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbOperatorTest.class);

  @SystemStub
  EnvironmentVariables environmentVariables =
      new EnvironmentVariables().set("MONGODB_CONNECTION_STRING", getMongoDbConnectionString());

  KubernetesMockServer server;
  KubernetesClient client;

  @BeforeAll
  static void beforeAll() throws IOException {
    startDb();
  }

  @AfterAll
  static void afterAll() {
    stopDb();
  }

  @BeforeEach
  void cleanAndSetup() {
    removeDatabases();
  }

  @AfterEach
  void log() throws InterruptedException {
    var lastRequest = server.getLastRequest();
    LOG.info("Last request path: {}", lastRequest.getPath());
  }

  @Test
  void shouldStartTheController() throws IOException {
    server
        .expect()
        .get()
        .withPath(
            "/apis/apiextensions.k8s.io/v1/customresourcedefinitions/mongodbs.persistence.sda-se.com")
        .andReturn(200, getCrd())
        .once();
    server
        .expect()
        .get()
        .withPath("/apis/persistence.sda-se.com/v1beta1/mongodbs?watch=true")
        .andReturn(200, new ArrayList<>())
        .always();

    var testThread = new Thread(() -> new MongoDbOperator(client));
    try {
      testThread.start();
      await().untilAsserted(() -> assertThat(server.getRequestCount()).isGreaterThan(2));
      assertThat(testThread.isAlive()).isTrue();
      assertThat(testThread.getState()).isEqualTo(Thread.State.WAITING);
    } finally {
      testThread.interrupt();
    }
  }

  private CustomResourceDefinition getCrd() throws IOException {
    var crdYaml =
        Files.readString(
            Path.of("kustomize/bases/operator/mongodbs-crd.yaml"), StandardCharsets.UTF_8);
    var crd =
        new ObjectMapper(new YAMLFactory()).readValue(crdYaml, CustomResourceDefinition.class);
    LOG.info("Read CRD {}", crd);
    return crd;
  }
}
