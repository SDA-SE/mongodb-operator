package com.sdase.k8s.operator.mongodb.controller.tasks.util;

/**
 * Exception to be thrown when no valid database or user name can be created from the {@link
 * com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource}.
 */
public class IllegalNameException extends IllegalStateException {
  public IllegalNameException(String message) {
    super(message);
  }
}
