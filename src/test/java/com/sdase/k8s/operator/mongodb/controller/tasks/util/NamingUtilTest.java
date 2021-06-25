package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.sdase.k8s.operator.mongodb.model.v1beta1.MongoDbCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.Test;

class NamingUtilTest {

  @Test
  void shouldCreateDatabaseName() {
    var given = new MongoDbCustomResource();
    given.setMetadata(
        new ObjectMetaBuilder().withNamespace("my-namespace").withName("my-name").build());

    var actual = NamingUtil.fromNamespaceAndName(given);

    assertThat(actual).isEqualTo("my-namespace_my-name");
  }

  @Test
  void shouldAllowMaxLengthOf63Characters() {
    var givenNamespace = "my-namesp1ce-with-r2ally-real3y-name";
    var givenName = "my-really1really-lo2g-name";
    assertThat(givenName.length() + givenNamespace.length()).isEqualTo(62);

    var given = new MongoDbCustomResource();
    given.setMetadata(
        new ObjectMetaBuilder().withNamespace(givenNamespace).withName(givenName).build());

    var actual = NamingUtil.fromNamespaceAndName(given);

    assertThat(actual).isEqualTo("my-namesp1ce-with-r2ally-real3y-name_my-really1really-lo2g-name");
  }

  @Test
  void shouldFailAtTotal64Characters() {
    var givenNamespace = "my-namesp1ce-with-r2ally-real3y-long-na4e";
    var givenName = "my-very-v1ry-long-n2me";
    assertThat(givenName.length() + givenNamespace.length()).isEqualTo(63);

    var given = new MongoDbCustomResource();
    given.setMetadata(
        new ObjectMetaBuilder().withNamespace(givenNamespace).withName(givenName).build());

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> NamingUtil.fromNamespaceAndName(given))
        .withMessageContaining("less than 64 characters");
  }
}
