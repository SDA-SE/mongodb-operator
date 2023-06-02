package com.sdase.k8s.operator.mongodb.monitoring;

public interface ReadinessCheck {

  /**
   * @return {@code true} if this check identifies the service as ready, {@code false} if not
   */
  boolean isReady();
}
