package com.sdase.k8s.operator.mongodb.ssl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SslUtilTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/ssl/custom/test.pem",
        "/ssl/le/le-ca-x3-iden-trust.pem",
        "/ssl/le/le-ca-x3-isrg-x1.pem"
      })
  void shouldReadCa(String pemResource) throws KeyStoreException, IOException {
    String pemContent = readPemContent(pemResource);

    KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

    assertThat(truststore).isNotNull();
    List<Certificate> certificates = extractCertificates(truststore);
    assertThat(certificates).hasSize(1);
  }

  @Test
  void shouldFailOnInvalidCertificate() throws IOException {
    String pemContent = readPemContent("/invalid.pem");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> SslUtil.createTruststoreFromPemKey(pemContent));
  }

  @Test
  void shouldReadCombinedCa() throws KeyStoreException, IOException {
    String pemContent = readPemContent("/combined.pem");

    KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

    assertThat(truststore).isNotNull();
    List<Certificate> certificates = extractCertificates(truststore);
    assertThat(certificates).hasSize(3);
  }

  @Test
  void shouldCreateSslContext() throws IOException {
    KeyStore givenTrustStore =
        SslUtil.createTruststoreFromPemKey(readPemContent("/ssl/custom/test.pem"));

    var sslContext = SslUtil.createSslContext(givenTrustStore);

    assertThat(sslContext).isNotNull().extracting(SSLContext::getProtocol).isEqualTo("TLSv1.2");
  }

  private String readPemContent(String pemResource) throws IOException {
    try (var pemContentStream = getClass().getResourceAsStream(pemResource)) {
      return new String(pemContentStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private List<Certificate> extractCertificates(KeyStore truststore) throws KeyStoreException {
    Enumeration<String> aliases = truststore.aliases();
    List<Certificate> certificates = new ArrayList<>();
    while (aliases.hasMoreElements()) {
      certificates.add(truststore.getCertificate(aliases.nextElement()));
    }
    return certificates;
  }
}
