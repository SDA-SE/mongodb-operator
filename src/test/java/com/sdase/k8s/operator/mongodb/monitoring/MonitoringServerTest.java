package com.sdase.k8s.operator.mongodb.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spark.Spark;

class MonitoringServerTest {

  private static int port;
  private static final AtomicBoolean READY = new AtomicBoolean(true);

  @BeforeAll
  static void setUp() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      port = serverSocket.getLocalPort();
      new MonitoringServer(port, List.of(READY::get)).start();
    }
  }

  @AfterAll
  static void tearDown() {
    Spark.stop();
  }

  @Test
  void shouldProvidePingEndpointForReadinessReady() {
    READY.set(true);
    var httpClient = new OkHttpClient();
    var request = pingEndpointRequest();
    await()
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

  @Test
  void shouldProvidePingEndpointForReadinessNotReady() {
    READY.set(false);
    var httpClient = new OkHttpClient();
    var request = pingEndpointRequest();
    await()
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

  private Request pingEndpointRequest() {
    var pingEndpoint = String.format("http://localhost:%d/health/readiness", port);
    return new Request.Builder().url(pingEndpoint).get().build();
  }
}
