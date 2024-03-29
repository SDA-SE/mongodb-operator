package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import com.mongodb.ConnectionString;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;

public class ConnectionStringUtil {

  private ConnectionStringUtil() {
    // utility class
  }

  /**
   * Overwrites a MongoDB {@linkplain ConnectionString} with the passed arguments.
   *
   * @param dbName the database name
   * @param username the username
   * @param password the password
   * @param customOptions custom connection string options, if {@code null} the default of the
   *     {@linkplain ConnectionString} is used.
   * @param connectionString the original connection
   * @return the created connection string
   */
  public static String createConnectionString(
      String dbName,
      String username,
      String password,
      String customOptions,
      ConnectionString connectionString) {
    var uri = URI.create(connectionString.getConnectionString());
    var hosts = String.join(",", connectionString.getHosts());
    return resolveConnectionString(
        dbName,
        username,
        password,
        customOptions != null ? customOptions : uri.getQuery(),
        uri,
        hosts);
  }

  private static String resolveConnectionString(
      String dbName,
      String username,
      String password,
      String customOptions,
      URI uri,
      String hosts) {
    return String.format(
        "%s://%s:%s@%s/%s".concat(StringUtils.isNotBlank(customOptions) ? "?%s" : ""),
        uri.getScheme(),
        encodeValue(username),
        encodeValue(password),
        hosts,
        encodeValue(dbName),
        customOptions);
  }

  private static String encodeValue(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
