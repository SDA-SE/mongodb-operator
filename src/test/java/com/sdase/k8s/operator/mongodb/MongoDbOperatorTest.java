package com.sdase.k8s.operator.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sdase.k8s.operator.mongodb.db.manager.AbstractMongoDbTest;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import java.io.IOException;
import java.lang.Thread.State;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;
import uk.org.webcompere.systemstubs.SystemStubs;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

@EnableKubernetesMockClient(crud = true)
class MongoDbOperatorTest extends AbstractMongoDbTest {

  private static final Logger LOG = LoggerFactory.getLogger(MongoDbOperatorTest.class);
  private static int port;

  EnvironmentVariables environmentVariables =
      SystemStubs.withEnvironmentVariables(
          "MONGODB_CONNECTION_STRING", getMongoDbConnectionString());

  KubernetesMockServer server;
  KubernetesClient client;

  @BeforeAll
  static void beforeAll() throws IOException {
    startDb();
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      port = serverSocket.getLocalPort();
    }
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
    var requestCount = server.getRequestCount();
    LOG.info("Request count: {}", requestCount);
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
        .andUpgradeToWebSocket()
        .open();

    var testThread =
        new Thread(
            () -> {
              try {
                environmentVariables.execute(() -> new MongoDbOperator(client, port));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    try {
      testThread.start();
      await().untilAsserted(() -> assertThat(server.getRequestCount()).isGreaterThan(1));
      await().untilAsserted(() -> assertThat(testThread.isAlive()).isTrue());
      await().untilAsserted(() -> assertThat(testThread.getState()).isEqualTo(State.WAITING));
      await().untilAsserted(this::assertLivenessEndpointAvailable);
      await().untilAsserted(this::assertReadinessEndpointAvailable);
    } finally {
      testThread.interrupt();
      Spark.stop();
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

  private void assertLivenessEndpointAvailable() {
    var pingEndpoint = String.format("http://localhost:%d/health/liveness", port);
    var request = new Request.Builder().url(pingEndpoint).get().build();
    var httpClient = new OkHttpClient();
    try (Response response = httpClient.newCall(request).execute()) {
      assertThat(response.isSuccessful()).isTrue();
    } catch (IOException e) {
      fail("Could not access liveness endpoint", e);
    }
  }

  private void assertReadinessEndpointAvailable() {
    var pingEndpoint = String.format("http://localhost:%d/health/readiness", port);
    var request = new Request.Builder().url(pingEndpoint).get().build();
    var httpClient = new OkHttpClient();
    try (Response response = httpClient.newCall(request).execute()) {
      assertThat(response.isSuccessful()).isTrue();
    } catch (IOException e) {
      fail("Could not access readiness endpoint", e);
    }
  }
}
