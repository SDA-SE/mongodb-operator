package com.sdase.k8s.operator.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.sdase.k8s.operator.mongodb.EnvironmentConfig.ConfigKeyResolver;
import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvironmentConfigTest {

  Map<String, String> givenEnvironment = new HashMap<>();
  ConfigKeyResolver configKeyResolver = givenEnvironment::get;

  @BeforeEach
  void clearGiven() {
    givenEnvironment.clear();
  }

  @Test
  void shouldFindMongoDbConfiguration() {
    givenEnvironment.put("MONGODB_CONNECTION_STRING", "mongodb://localhost");
    var actual = new EnvironmentConfig(configKeyResolver);

    assertThat(actual.getMongodbConnectionString()).isEqualTo("mongodb://localhost");
  }

  @Test
  void shouldRejectEmptyMongoDbConfiguration() {
    givenEnvironment.put("MONGODB_CONNECTION_STRING", "   ");
    assertThatExceptionOfType(ValidationException.class)
        .isThrownBy(() -> new EnvironmentConfig(configKeyResolver));
  }

  @Test
  void shouldUseDefaultTrustedCertificatesDir() {
    givenEnvironment.put("MONGODB_CONNECTION_STRING", "mongodb://only.to.avoid.failure");
    var environmentConfig = new EnvironmentConfig(configKeyResolver);

    assertThat(environmentConfig)
        .extracting(EnvironmentConfig::getTrustedCertificatesDir)
        .isEqualTo("/var/trust/certificates");
  }

  @Test
  void shouldConfigureTrustedCertificatesDir() {
    givenEnvironment.putAll(
        Map.of(
            "MONGODB_CONNECTION_STRING",
            "mongodb://only.to.avoid.failure",
            "TRUSTED_CERTIFICATES_DIR",
            "/var/example/test"));
    var environmentConfig = new EnvironmentConfig(configKeyResolver);

    assertThat(environmentConfig)
        .extracting(EnvironmentConfig::getTrustedCertificatesDir)
        .isEqualTo("/var/example/test");
  }

  @Test
  void shouldRejectAbsentMongoDbConfiguration() {
    assertThatExceptionOfType(ValidationException.class).isThrownBy(EnvironmentConfig::new);
  }
}
