package com.sdase.k8s.operator.mongodb.monitoring;

import java.util.function.Supplier;
import spark.Spark;

public class MonitoringServer {

  private final int port;
  private final Supplier<Boolean> isReady;

  public MonitoringServer(int port, Supplier<Boolean> isReady) {
    this.port = port;
    this.isReady = isReady;
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
