package com.sdase.k8s.operator.mongodb;

import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentConfig {

  private static final Logger LOG = LoggerFactory.getLogger(EnvironmentConfig.class);

  @NotBlank private String mongodbConnectionString;

  public EnvironmentConfig() {
    createConfig();
    validateConfig();
  }

  public String getMongodbConnectionString() {
    return mongodbConnectionString;
  }

  private void createConfig() {
    mongodbConnectionString = System.getenv("MONGODB_CONNECTION_STRING");
  }

  private void validateConfig() {
    var constraintViolations =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator()
            .validate(this);
    if (constraintViolations.isEmpty()) {
      LOG.info("Config is valid.");
      return;
    }
    constraintViolations.forEach(
        cv -> LOG.info("{} is invalid: {}", cv.getPropertyPath(), cv.getMessage()));
    throw new ValidationException("Invalid configuration. Check log for further information.");
  }
}
