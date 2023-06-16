package com.sdase.k8s.operator.mongodb.monitoring;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

public class MonitoringServer implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(MonitoringServer.class);

  private static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
  private final int port;
  private final Supplier<Boolean> isReady;
  private final Service service;
  private final MeterRegistry meterRegistry;
  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final List<AutoCloseable> closeOnShutdown = new ArrayList<>();

  public MonitoringServer(int port, Collection<ReadinessCheck> readinessChecks) {
    this.port = port;
    this.isReady = () -> readinessChecks.stream().allMatch(ReadinessCheck::isReady);
    this.service = Service.ignite();
    this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    this.meterRegistry = new CompositeMeterRegistry(Clock.SYSTEM, List.of(prometheusMeterRegistry));
    this.meterRegistry.config().commonTags("application", "mongodb-operator");
    setupJvmMetrics();
  }

  public MonitoringServer start() {

    service.port(port);

    service.get(
        "/health/readiness",
        (req, res) -> {
          res.type(MIME_TYPE_TEXT_PLAIN);
          var ready = Boolean.TRUE.equals(this.isReady.get());
          res.status(ready ? 200 : 503);
          return ready ? "UP" : "OUT_OF_SERVICE";
        });

    service.get(
        "/health/liveness",
        (req, res) -> {
          res.type(MIME_TYPE_TEXT_PLAIN);
          res.status(200);
          return "UP";
        });

    service.get(
        "/metrics/prometheus",
        (req, res) -> {
          String response = prometheusMeterRegistry.scrape();
          res.type(MIME_TYPE_TEXT_PLAIN);
          res.status(200);
          return response;
        });

    closeOnShutdown.add(
        () -> {
          service.stop();
          service.awaitStop();
        });

    return this;
  }

  public void stop() {
    closeOnShutdown.forEach(
        it -> {
          try {
            it.close();
          } catch (Exception e) {
            LOG.warn("Ignoring failure when closing {} on shutdown.", it, e);
          }
        });
  }

  private void setupJvmMetrics() {
    Stream.of(
            new ClassLoaderMetrics(),
            new JvmMemoryMetrics(),
            new JvmGcMetrics(),
            new ProcessorMetrics(),
            new JvmThreadMetrics())
        .forEach(
            meterBinder -> {
              meterBinder.bindTo(meterRegistry);
              if (meterBinder instanceof AutoCloseable autoCloseable) {
                closeOnShutdown.add(autoCloseable);
              }
            });
  }

  @Override
  public void close() {
    stop();
  }
}
