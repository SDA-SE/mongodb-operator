package com.sdase.k8s.operator.mongodb.ssl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateCollector {

  private static final Logger LOG = LoggerFactory.getLogger(CertificateCollector.class);
  private static final int MAX_RECURSIVE_DEPTH = 10;

  private final String pathToDirectoryWithCertificates;

  /**
   * @param pathToDirectoryWithCertificates the directory where *.pem with CA certificates are
   *     located. The path is scanned recursively. All files but *.pem files are ignored. The method
   *     will fail, if the path is not readable but will ignore if it is not a directory or does not
   *     exist.
   */
  public CertificateCollector(String pathToDirectoryWithCertificates) {
    this.pathToDirectoryWithCertificates = pathToDirectoryWithCertificates;
  }

  public Optional<String> readCertificates() {
    var optionalPath = toPathIfExists(pathToDirectoryWithCertificates);
    if (optionalPath.isEmpty()) {
      LOG.info(
          "Not collecting CA certificates, {} does not exist or is not a directory.",
          pathToDirectoryWithCertificates);
      return Optional.empty();
    }
    LOG.info("Checking for CA certificates in {}", pathToDirectoryWithCertificates);
    return findAllPemContent(optionalPath.get());
  }

  @SuppressWarnings("java:S3864") // peek is fine for debug logs
  private Optional<String> findAllPemContent(Path path) {
    try (Stream<Path> pathStream = Files.walk(path, MAX_RECURSIVE_DEPTH)) {
      return Optional.of(
              pathStream
                  .peek(p -> LOG.debug("Checking {}", p))
                  .filter(Files::isRegularFile)
                  .filter(this::isPemFile)
                  .map(this::readContent)
                  .collect(Collectors.joining("\n\n")))
          .filter(StringUtils::isNotBlank);
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to read pem files from %s", path), e);
    }
  }

  private boolean isPemFile(Path filePath) {
    var isPemFile = filePath.getFileName().toString().endsWith(".pem");
    if (!isPemFile) {
      LOG.info("Omitting {}: not a .pem file", filePath);
    }
    return isPemFile;
  }

  private String readContent(Path pemFile) {
    try {
      return Files.readString(pemFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to read %s", pemFile), e);
    }
  }

  private Optional<Path> toPathIfExists(String location) {
    var optionalPath =
        Optional.of(Path.of(location)).filter(Files::exists).filter(Files::isDirectory);
    if (optionalPath.isEmpty()) {
      return optionalPath;
    } else if (!Files.isReadable(optionalPath.get())) {
      throw new IllegalStateException(
          String.format("Existing directory %s is not readable.", pathToDirectoryWithCertificates));
    }
    return optionalPath;
  }
}
