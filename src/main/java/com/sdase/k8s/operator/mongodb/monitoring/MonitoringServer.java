package com.sdase.k8s.operator.mongodb.monitoring;

import io.javalin.Javalin;
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

public class MonitoringServer implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(MonitoringServer.class);

  private static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
  private final int port;
  private final Supplier<Boolean> isReady;
  private final Javalin server;
  private final MeterRegistry meterRegistry;
  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final List<AutoCloseable> closeOnShutdown = new ArrayList<>();

  public MonitoringServer(int port, Collection<ReadinessCheck> readinessChecks) {
    this.port = port;
    this.isReady = () -> readinessChecks.stream().allMatch(ReadinessCheck::isReady);
    this.server = Javalin.create();
    this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    this.meterRegistry = new CompositeMeterRegistry(Clock.SYSTEM, List.of(prometheusMeterRegistry));
    this.meterRegistry.config().commonTags("application", "mongodb-operator");
    setupJvmMetrics();
  }

  public MonitoringServer start() {

    server.start(port);

    server.get(
        "/health/readiness",
        ctx -> {
          ctx.contentType(MIME_TYPE_TEXT_PLAIN);
          var ready = Boolean.TRUE.equals(this.isReady.get());
          ctx.status(ready ? 200 : 503);
          ctx.result(ready ? "UP" : "OUT_OF_SERVICE");
        });

    server.get(
        "/health/liveness",
        ctx -> {
          ctx.contentType(MIME_TYPE_TEXT_PLAIN);
          ctx.status(200);
          ctx.result("UP");
        });

    server.get(
        "/metrics/prometheus",
        ctx -> {
          String response = prometheusMeterRegistry.scrape();
          ctx.contentType(MIME_TYPE_TEXT_PLAIN);
          ctx.result(response);
        });

    closeOnShutdown.add(server::stop);

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
