package com.sdase.k8s.operator.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.junit.jupiter.api.Test;

class KeepAliveRunnerTest {

  @Test
  void shouldExitProperly() {

    var keepAliveRunner = new KeepAliveRunner();

    var thread = new Thread(keepAliveRunner::keepAlive);

    thread.start();

    await().untilAsserted(() -> assertThat(thread.isAlive()).isTrue());

    thread.interrupt();

    await().untilAsserted(() -> assertThat(thread.isAlive()).isFalse());
  }
}
