package com.sdase.k8s.operator.mongodb;

import com.sdase.k8s.operator.mongodb.controller.KubernetesClientAdapter;
import com.sdase.k8s.operator.mongodb.controller.MongoDbController;
import com.sdase.k8s.operator.mongodb.controller.V1SecretBuilder;
import com.sdase.k8s.operator.mongodb.controller.tasks.TaskFactory;
import com.sdase.k8s.operator.mongodb.db.manager.MongoDbService;
import com.sdase.k8s.operator.mongodb.monitoring.MonitoringServer;
import com.sdase.k8s.operator.mongodb.ssl.CertificateCollector;
import com.sdase.k8s.operator.mongodb.ssl.util.SslUtil;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import java.util.Optional;
import javax.net.ssl.SSLContext;

public class MongoDbOperator {

  public MongoDbOperator(KubernetesClient kubernetesClient, int monitoringPort) {
    try (var operator = new Operator(kubernetesClient, DefaultConfigurationService.instance())) {
      operator.register(createMongoDbController(kubernetesClient));
      operator.start(); // adds some checks and produces some logs, exits on error
      startMonitoringServer(monitoringPort);
      keepRunning();
    }
  }

  private MongoDbController createMongoDbController(KubernetesClient kubernetesClient) {
    var config = new EnvironmentConfig();
    var mongoDbService =
        createSslContext(config.getTrustedCertificatesDir())
            .map(sc -> new MongoDbService(config.getMongodbConnectionString(), sc))
            .orElseGet(() -> new MongoDbService(config.getMongodbConnectionString()));
    return new MongoDbController(
        new KubernetesClientAdapter(kubernetesClient),
        TaskFactory.defaultFactory(),
        mongoDbService,
        new V1SecretBuilder());
  }

  private Optional<SSLContext> createSslContext(String trustedCertificatesDir) {
    var certificateCollector = new CertificateCollector(trustedCertificatesDir);
    final Optional<String> certificates = certificateCollector.readCertificates();
    return certificates.map(SslUtil::createTruststoreFromPemKey).map(SslUtil::createSslContext);
  }

  private void startMonitoringServer(int port) {
    new MonitoringServer(port, () -> true).start();
  }

  /*
   * This should not be needed with the running MonitoringServer. But when removing this and not
   * using the try with resources of the new operator in the constructor, there are rejected
   * Kubernetes API watchers flooding the log and the Operator is not working as expected. There
   * seems to be a similar problem when using Quarkus which produces the same type of exceptions,
   * see https://github.com/quarkiverse/quarkus-operator-sdk/issues/9
   */
  private void keepRunning() {
    new KeepAliveRunner().keepAlive();
  }

  public static void main(String[] args) {
    try (var client = new DefaultKubernetesClient()) {
      new MongoDbOperator(client, 8081);
    }
  }
}
