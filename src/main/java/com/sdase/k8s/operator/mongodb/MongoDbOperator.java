package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.KubernetesClientAdapter;
import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.controller.V1SecretBuilder;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.logging.LogConfigurer;
import com.sdase.k8s.operator.mongodb.monitoring.MongoDbPrivilegesCheck;
import com.sdase.k8s.operator.mongodb.monitoring.MonitoringServer;
import com.sdase.k8s.operator.mongodb.monitoring.ReadinessCheck;
import com.sdase.k8s.operator.mongodb.ssl.CertificateCollector;
import com.sdase.k8s.operator.mongodb.ssl.util.SslUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationServiceProvider;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLContext;

public class MongoDbOperator {

  public MongoDbOperator(
      EnvironmentConfig config, KubernetesClient kubernetesClient, int monitoringPort) {
    var operator = new Operator(kubernetesClient, ConfigurationServiceProvider.instance());
    // timeout as in ConfigurationService.DEFAULT_TERMINATION_TIMEOUT_SECONDS
    operator.installShutdownHook(Duration.ofSeconds(10L));
    var mongoDbService = createMongoDbService(config);
    var mongoDbPrivilegesCheck = verifyPrivileges(mongoDbService);
    operator.register(createMongoDbController(kubernetesClient, mongoDbService));
    operator.start(); // adds some checks and produces some logs, exits on error
    var monitoringServer = startMonitoringServer(monitoringPort, mongoDbPrivilegesCheck);
    keepRunning(monitoringServer);
  }

  private MongoDbPrivilegesCheck verifyPrivileges(MongoDbService mongoDbService) {
    return new MongoDbPrivilegesCheck(mongoDbService);
  }

  private MongoDbController createMongoDbController(
      KubernetesClient kubernetesClient, MongoDbService mongoDbService) {
    return new MongoDbController(
        new KubernetesClientAdapter(kubernetesClient),
        TaskFactory.defaultFactory(),
        mongoDbService,
        new V1SecretBuilder());
  }

  private MongoDbService createMongoDbService(EnvironmentConfig config) {
    return createSslContext(config.getTrustedCertificatesDir())
        .map(sc -> new MongoDbService(config.getMongodbConnectionString(), sc))
        .orElseGet(() -> new MongoDbService(config.getMongodbConnectionString()));
  }

  private Optional<SSLContext> createSslContext(String trustedCertificatesDir) {
    var certificateCollector = new CertificateCollector(trustedCertificatesDir);
    final Optional<String> certificates = certificateCollector.readCertificates();
    return certificates.map(SslUtil::createTruststoreFromPemKey).map(SslUtil::createSslContext);
  }

  @SuppressWarnings({"java:S2095", "resource"}) // MonitoringServer is closed by caller
  private MonitoringServer startMonitoringServer(int port, ReadinessCheck... readinessChecks) {
    return new MonitoringServer(port, List.of(readinessChecks)).start();
  }

  /*
   * This should not be needed with the running MonitoringServer. But when removing this and not
   * using the try with resources of the new operator in the constructor, there are rejected
   * Kubernetes API watchers flooding the log and the Operator is not working as expected. There
   * seems to be a similar problem when using Quarkus which produces the same type of exceptions,
   * see https://github.com/quarkiverse/quarkus-operator-sdk/issues/9
   *
   * @param toBeClosedAfterFinish resources that should be closed when the service is shutting down
   */
  private void keepRunning(AutoCloseable... toBeClosedAfterFinish) {
    new KeepAliveRunner(toBeClosedAfterFinish).keepAlive();
  }

  public static void main(String[] args) {
    var config = new EnvironmentConfig();
    LogConfigurer.configure(config.isEnableJsonLogging());
    try (var client = new KubernetesClientBuilder().build()) {
      new MongoDbOperator(config, client, 8081);
    }
  }
}
