package com.sdase.k8s.operator.mongodb;

import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentConfig {

  private static final Logger LOG = LoggerFactory.getLogger(EnvironmentConfig.class);
  private static final String DEFAULT_TRUSTED_CERTIFICATES_DIR = "/var/trust/certificates";

  @NotBlank private String mongodbConnectionString;

  private String trustedCertificatesDir;

  public EnvironmentConfig() {
    createConfig();
    validateConfig();
  }

  public String getMongodbConnectionString() {
    return mongodbConnectionString;
  }

  public String getTrustedCertificatesDir() {
    return trustedCertificatesDir;
  }

  private void createConfig() {
    mongodbConnectionString = System.getenv("MONGODB_CONNECTION_STRING");
    trustedCertificatesDir = System.getenv("TRUSTED_CERTIFICATES_DIR");
    if (StringUtils.isBlank(trustedCertificatesDir)) {
      trustedCertificatesDir = DEFAULT_TRUSTED_CERTIFICATES_DIR;
    }
  }

  private void validateConfig() {
    try (var validatorFactory =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()) {
      var constraintViolations = validatorFactory.getValidator().validate(this);
      if (constraintViolations.isEmpty()) {
        LOG.info("Config is valid.");
        return;
      }
      constraintViolations.forEach(
          cv -> LOG.info("{} is invalid: {}", cv.getPropertyPath(), cv.getMessage()));
      throw new ValidationException("Invalid configuration. Check log for further information.");
    }
  }
}
