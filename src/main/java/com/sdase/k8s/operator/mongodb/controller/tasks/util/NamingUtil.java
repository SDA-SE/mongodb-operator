package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;

public class NamingUtil {
  private NamingUtil() {
    // utility class
  }

  public static String fromNamespaceAndName(MongoDbCustomResource mongoDbCustomResource)
      throws IllegalNameException {
    final String namespace = mongoDbCustomResource.getMetadata().getNamespace();
    final String name = mongoDbCustomResource.getMetadata().getName();
    var result = String.join("_", namespace, name);
    if (result.length() < 64) {
      return result;
    }
    throw new IllegalNameException(
        String.format(
            "Unable to create valid name with less than 64 characters "
                + "from namespace %s and name %s",
            namespace, name));
  }
}
