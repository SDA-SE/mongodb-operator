package com.sdase.k8s.operator.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class EnvironmentConfigTest {

  @Test
  @SetEnvironmentVariable(key = "MONGODB_CONNECTION_STRING", value = "mongodb://localhost")
  void shouldFindMongoDbConfiguration() {
    var actual = new EnvironmentConfig();

    assertThat(actual.getMongodbConnectionString()).isEqualTo("mongodb://localhost");
  }

  @Test
  @SetEnvironmentVariable(key = "MONGODB_CONNECTION_STRING", value = "   ")
  void shouldRejectEmptyMongoDbConfiguration() {
    assertThatExceptionOfType(ValidationException.class).isThrownBy(EnvironmentConfig::new);
  }

  @Test
  @SetEnvironmentVariable(
      key = "MONGODB_CONNECTION_STRING",
      value = "mongodb://only.to.avoid.failure")
  @ClearEnvironmentVariable(key = "TRUSTED_CERTIFICATES_DIR")
  void shouldUseDefaultTrustedCertificatesDir() {
    var environmentConfig = new EnvironmentConfig();

    assertThat(environmentConfig)
        .extracting(EnvironmentConfig::getTrustedCertificatesDir)
        .isEqualTo("/var/trust/certificates");
  }

  @Test
  @SetEnvironmentVariable(
      key = "MONGODB_CONNECTION_STRING",
      value = "mongodb://only.to.avoid.failure")
  @SetEnvironmentVariable(key = "TRUSTED_CERTIFICATES_DIR", value = "/var/example/test")
  void shouldConfigureTrustedCertificatesDir() {
    var environmentConfig = new EnvironmentConfig();

    assertThat(environmentConfig)
        .extracting(EnvironmentConfig::getTrustedCertificatesDir)
        .isEqualTo("/var/example/test");
  }

  @Test
  @ClearEnvironmentVariable(key = "MONGODB_CONNECTION_STRING")
  void shouldRejectAbsentMongoDbConfiguration() {
    assertThatExceptionOfType(ValidationException.class).isThrownBy(EnvironmentConfig::new);
  }
}
