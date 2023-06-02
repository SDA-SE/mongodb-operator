package com.sdase.k8s.operator.mongodb.ssl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CertificateCollectorTest {

  @TempDir Path tempDir;

  @Test
  void shouldFindAllCertificatesRecursively() throws URISyntaxException {

    var givenPathToDirectoryWithCertificates =
        Path.of(getClass().getResource("/ssl").toURI()).toString();

    var certificateCollector = new CertificateCollector(givenPathToDirectoryWithCertificates);
    var actualCertificatesOptional = certificateCollector.readCertificates();

    assertThat(actualCertificatesOptional)
        .isPresent()
        .hasValueSatisfying(
            content ->
                assertThat(content)
                    .contains(readPemContent("/ssl/custom/test.pem"))
                    .contains(readPemContent("/ssl/le/le-ca-x3-iden-trust.pem"))
                    .contains(readPemContent("/ssl/le/le-ca-x3-isrg-x1.pem")));
  }

  @Test
  void shouldOnlyCollectCertificatesRecursively() throws URISyntaxException {

    var givenPathToDirectoryWithCertificates =
        Path.of(getClass().getResource("/ssl").toURI()).toString();

    var certificateCollector = new CertificateCollector(givenPathToDirectoryWithCertificates);
    var actualCertificatesOptional = certificateCollector.readCertificates();

    assertThat(actualCertificatesOptional)
        .isPresent()
        .hasValueSatisfying(
            content -> assertThat(content).doesNotContain("This should not be collected."));
  }

  @Test
  void shouldNotFailIfPathDoesNotExist() {
    var givenDirThatDoesNotExist =
        tempDir.resolve("does").resolve("not").resolve("exist").toString();

    var certificateCollector = new CertificateCollector(givenDirThatDoesNotExist);
    assertThatNoException().isThrownBy(certificateCollector::readCertificates);
  }

  @Test
  void shouldNotFailNotReadable() throws IOException {
    var notReadableDir = tempDir.resolve("notReadable");
    Files.createDirectory(notReadableDir);
    var originalPermissions = Files.getPosixFilePermissions(notReadableDir);
    try {
      Files.setPosixFilePermissions(notReadableDir, Set.of(PosixFilePermission.OWNER_WRITE));
      var givenDirThatDoesNotExist = notReadableDir.toString();

      var certificateCollector = new CertificateCollector(givenDirThatDoesNotExist);
      assertThatExceptionOfType(IllegalStateException.class)
          .isThrownBy(certificateCollector::readCertificates);
    } finally {
      Files.setPosixFilePermissions(notReadableDir, originalPermissions);
    }
  }

  private String readPemContent(String pemResource) {
    try (var pemContentStream = getClass().getResourceAsStream(pemResource)) {
      return new String(pemContentStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
