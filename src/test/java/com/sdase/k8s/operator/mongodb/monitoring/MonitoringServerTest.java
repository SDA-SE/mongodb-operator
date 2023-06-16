package com.sdase.k8s.operator.mongodb.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junitpioneer.jupiter.RetryingTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MonitoringServerTest {

  private static final Logger LOG = LoggerFactory.getLogger(MonitoringServerTest.class);

  private MonitoringServer monitoringServer;
  private int port;
  private final AtomicBoolean ready = new AtomicBoolean(true);

  @BeforeEach
  void startServer() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      port = serverSocket.getLocalPort();
      monitoringServer = new MonitoringServer(port, List.of(ready::get)).start();
    }
  }

  @AfterEach
  void stopServer() {
    monitoringServer.stop();
  }

  @RetryingTest(5)
  void shouldProvidePingEndpointForReadinessReady() {
    ready.set(true);
    var httpClient = new OkHttpClient();
    var request = readinessEndpointRequest();
    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.header("Content-type")).isEqualTo("text/plain");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body()).isNotNull();
                assertThat(response.body().string()).isEqualTo("UP");
              }
            });
  }

  @RetryingTest(5)
  void shouldProvidePingEndpointForReadinessNotReady() {
    ready.set(false);
    var httpClient = new OkHttpClient();
    var request = readinessEndpointRequest();
    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.header("Content-type")).isEqualTo("text/plain");
                assertThat(response.code()).isEqualTo(503);
                assertThat(response.body()).isNotNull();
                assertThat(response.body().string()).isEqualTo("OUT_OF_SERVICE");
              }
            });
  }

  @RetryingTest(5)
  void shouldProvidePingEndpointForLiveness() {
    var httpClient = new OkHttpClient();
    var request = livenessEndpointRequest();
    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.header("Content-type")).isEqualTo("text/plain");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body()).isNotNull();
                assertThat(response.body().string()).isEqualTo("UP");
              }
            });
  }

  @RetryingTest(5)
  void shouldProvideMetricsPrometheusEndpoint() {
    var httpClient = new OkHttpClient();
    var request = metricsEndpointRequest();
    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.header("Content-type")).isEqualTo("text/plain");
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body()).isNotNull();
                String responseBody = response.body().string();
                LOG.info("Scraped metrics:\n{}", responseBody);
                assertThat(responseBody).contains("jvm_memory_max_bytes");
                assertThat(responseBody.lines().filter(l -> !l.startsWith("#")))
                    .allSatisfy(l -> assertThat(l).contains("application=\"mongodb-operator\""));
              }
            });
  }

  private Request readinessEndpointRequest() {
    var pingEndpoint = String.format("http://localhost:%d/health/readiness", port);
    return new Request.Builder().url(pingEndpoint).get().build();
  }

  private Request livenessEndpointRequest() {
    var pingEndpoint = String.format("http://localhost:%d/health/liveness", port);
    return new Request.Builder().url(pingEndpoint).get().build();
  }

  private Request metricsEndpointRequest() {
    var metricsEndpoint = String.format("http://localhost:%d/metrics/prometheus", port);
    return new Request.Builder().url(metricsEndpoint).get().build();
  }
}
