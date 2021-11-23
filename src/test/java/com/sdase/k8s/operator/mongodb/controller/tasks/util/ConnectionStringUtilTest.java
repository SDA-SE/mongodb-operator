package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnectionStringUtilTest {

  @Test
  void shouldCreateConnectionString() {
    var result =
        ConnectionStringUtil.createConnectionString(
            "testDb", "testUser", "s3cret", "mongodb0.example.com:27017", null);

    assertThat(result).isEqualTo("mongodb://testUser:s3cret@mongodb0.example.com:27017/testDb");
  }

  @Test
  void shouldCreateConnectionStringWithConnectionStringOptions() {
    var result =
        ConnectionStringUtil.createConnectionString(
            "testDb",
            "testUser",
            "s3cret",
            "mongodb0.example.com:27017",
            "readPreference=secondaryPreferred&retryWrites=false");

    assertThat(result)
        .isEqualTo(
            "mongodb://testUser:s3cret@mongodb0.example.com:27017/testDb?readPreference=secondaryPreferred&retryWrites=false");
  }
}
