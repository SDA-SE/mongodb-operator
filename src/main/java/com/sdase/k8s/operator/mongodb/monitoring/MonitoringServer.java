package com.sdase.k8s.operator.mongodb.monitoring;

import java.util.Collection;
import java.util.function.Supplier;
import spark.Spark;

public class MonitoringServer {

  private final int port;
  private final Supplier<Boolean> isReady;

  public MonitoringServer(int port, Collection<ReadinessCheck> readinessChecks) {
    this.port = port;
    this.isReady = () -> readinessChecks.stream().allMatch(ReadinessCheck::isReady);
  }

  public void start() {

    Spark.port(port);

    Spark.get(
        "/health/readiness",
        (req, res) -> {
          res.type("text/plain");
          var ready = Boolean.TRUE.equals(this.isReady.get());
          res.status(ready ? 200 : 503);
          return ready ? "UP" : "OUT_OF_SERVICE";
        });
  }
}
