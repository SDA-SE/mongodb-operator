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
  @ClearEnvironmentVariable(key = "MONGODB_CONNECTION_STRING")
  void shouldRejectAbsentMongoDbConfiguration() {
    assertThatExceptionOfType(ValidationException.class).isThrownBy(EnvironmentConfig::new);
  }
}
