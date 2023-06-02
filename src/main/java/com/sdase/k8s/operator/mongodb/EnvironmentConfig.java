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

  private final ConfigKeyResolver configKeyResolver;

  public EnvironmentConfig() {
    this(System::getenv);
  }

  // internally used for testing without mocking System.getenv()
  EnvironmentConfig(ConfigKeyResolver configKeyResolver) {
    this.configKeyResolver = configKeyResolver;
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
    mongodbConnectionString = configKeyResolver.getValue("MONGODB_CONNECTION_STRING");
    trustedCertificatesDir = configKeyResolver.getValue("TRUSTED_CERTIFICATES_DIR");
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

  interface ConfigKeyResolver {
    /**
     * Retrieves the value for the given {@code key} as configured in the current environment. By
     * {@linkplain EnvironmentConfig#EnvironmentConfig() default}, the {@linkplain
     * System#getenv(String) system environment variables} are used for lookup.
     *
     * @param key the config key
     * @return the value or {@code null} if not defined
     */
    String getValue(String key);
  }
}
