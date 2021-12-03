package com.sdase.k8s.operator.mongodb.monitoring;

import java.util.Collection;
import java.util.function.Supplier;
import spark.Service;

public class MonitoringServer implements AutoCloseable {

  private final int port;
  private final Supplier<Boolean> isReady;
  private final Service service;

  public MonitoringServer(int port, Collection<ReadinessCheck> readinessChecks) {
    this.port = port;
    this.isReady = () -> readinessChecks.stream().allMatch(ReadinessCheck::isReady);
    this.service = Service.ignite();
  }

  public MonitoringServer start() {

    service.port(port);

    service.get(
        "/health/readiness",
        (req, res) -> {
          res.type("text/plain");
          var ready = Boolean.TRUE.equals(this.isReady.get());
          res.status(ready ? 200 : 503);
          return ready ? "UP" : "OUT_OF_SERVICE";
        });

    service.get(
        "/health/liveness",
        (req, res) -> {
          res.type("text/plain");
          res.status(200);
          return "UP";
        });

    return this;
  }

  public void stop() {
    service.stop();
    service.awaitStop();
  }

  @Override
  public void close() {
    stop();
  }
}
