package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import org.apache.commons.lang3.StringUtils;

public class ConnectionStringUtil {

  private static final String CONNECTION_STRING_TEMPLATE = "mongodb://%s:%s@%s/%s";

  private ConnectionStringUtil() {
    // utility class
  }

  public static String createConnectionString(
      String dbName, String username, String password, String hosts, String options) {
    final var connectionString =
        String.format(CONNECTION_STRING_TEMPLATE, username, password, hosts, dbName);
    return StringUtils.isNotBlank(options)
        ? connectionString.concat("?").concat(options)
        : connectionString;
  }
}
