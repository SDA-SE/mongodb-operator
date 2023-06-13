package com.sdase.k8s.operator.mongodb;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdase.k8s.operator.mongodb.logging.LogConfigurer;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@SuppressWarnings("JUnitMalformedDeclaration") // StdOut param is unexpected
class LoggingTest {

  static final Logger LOG = LoggerFactory.getLogger(LoggingTest.class);
  static final ObjectMapper OM = new ObjectMapper();

  @AfterEach
  void reset() {
    LogConfigurer.configure(false);
  }

  @Test
  @StdIo
  void shouldLogRegularInfo(StdOut out) {
    LogConfigurer.configure(false);
    var message = "Hello this is an info from shouldLogRegularInfo.";
    LOG.info(message);
    await()
        .untilAsserted(
            () -> {
              var log = stream(out.capturedLines()).filter(s -> s.contains(message)).findFirst();
              assertThat(log)
                  .isPresent()
                  .get()
                  .asString()
                  .contains(" [Test worker] INFO " + getClass().getName() + " -- " + message)
                  .matches("^[\\d.:]+ \\[.*");
            });
  }

  @Test
  @StdIo
  void shouldLogRegularException(StdOut out) {
    LogConfigurer.configure(false);
    var message = "Hello this is a warning from shouldLogRegularException.";
    var exception = new TestException("Exception in shouldLogRegularException");
    LOG.warn(message, exception);
    await()
        .untilAsserted(
            () -> {
              var log = String.join("\n", out.capturedLines());
              assertThat(log)
                  .contains(
                      " [Test worker] WARN "
                          + getClass().getName()
                          + " -- "
                          + message
                          + "\n"
                          + "com.sdase.k8s.operator.mongodb.LoggingTest$TestException: Exception in shouldLogRegularException\n"
                          + "\tat app//com.sdase.k8s.operator.mongodb.LoggingTest.shouldLogRegularException(LoggingTest.java:100)");
            });
  }

  @Test
  @StdIo
  void shouldLogInfoAsJson(StdOut out) {
    LogConfigurer.configure(true);
    String message = "This is a log test form shouldLogInfoAsJson.";
    LOG.info(message);
    await()
        .untilAsserted(
            () -> {
              var log = stream(out.capturedLines()).filter(l -> l.contains(message)).findFirst();
              assertThat(log).isPresent().get().asString().startsWith("{");
              var actualLogStructure =
                  OM.readValue(log.get(), new TypeReference<Map<String, Object>>() {});
              assertThat(actualLogStructure)
                  .containsEntry("level", "INFO")
                  .containsEntry("message", message)
                  .containsEntry("logger", this.getClass().getName());
            });
  }

  @Test
  @StdIo
  void shouldLogMdcAsJson(StdOut out) {
    LogConfigurer.configure(true);
    try (var ignored = MDC.putCloseable("test_method", "shouldLogMdcAsJson")) {
      String message = "This is a log test form shouldLogMdcAsJson.";
      LOG.warn(message);
      await()
          .untilAsserted(
              () -> {
                var log = stream(out.capturedLines()).filter(l -> l.contains(message)).findFirst();
                assertThat(log).isPresent().get().asString().startsWith("{");
                var actualLogStructure =
                    OM.readValue(log.get(), new TypeReference<Map<String, Object>>() {});
                assertThat(actualLogStructure)
                    .containsEntry("level", "WARN")
                    .containsEntry("message", message)
                    .containsEntry("logger", this.getClass().getName())
                    .containsEntry("mdc", Map.of("test_method", "shouldLogMdcAsJson"));
              });
    }
  }

  @Test
  @StdIo
  void shouldLogExceptionAsJson(StdOut out) {
    LogConfigurer.configure(true);
    String message = "This is a log test form shouldLogExceptionAsJson.";
    RuntimeException runtimeException = new TestException("Hui, this is an error!");
    LOG.warn(message, runtimeException);
    await()
        .untilAsserted(
            () -> {
              var log = stream(out.capturedLines()).filter(l -> l.contains(message)).findFirst();
              assertThat(log).isPresent().get().asString().startsWith("{");
              var actualLogStructure =
                  OM.readValue(log.get(), new TypeReference<Map<String, Object>>() {});
              assertThat(actualLogStructure)
                  .containsEntry("level", "WARN")
                  .containsEntry("message", message)
                  .containsEntry("logger", this.getClass().getName())
                  .containsEntry(
                      "exception",
                      """
                          com.sdase.k8s.operator.mongodb.LoggingTest$TestException: Hui, this is an error!
                          \tat app//com.sdase.k8s.operator.mongodb.LoggingTest.shouldLogExceptionAsJson(LoggingTest.java:100)
                          """);
            });
  }

  @Test
  @StdIo
  void shouldLogInfoAndAboveJson(StdOut out) {
    LogConfigurer.configure(true);
    LOG.trace("trace shouldLogInfoAndAbove");
    LOG.debug("debug shouldLogInfoAndAbove");
    LOG.info("info shouldLogInfoAndAbove");
    LOG.warn("warn shouldLogInfoAndAbove");
    LOG.error("error shouldLogInfoAndAbove");
    await()
        .untilAsserted(
            () ->
                assertThat(StringUtils.join(out.capturedLines(), "\n"))
                    .contains("error shouldLogInfoAndAbove")
                    .contains("warn shouldLogInfoAndAbove")
                    .contains("info shouldLogInfoAndAbove")
                    .doesNotContain("debug shouldLogInfoAndAbove")
                    .doesNotContain("trace shouldLogInfoAndAbove"));
  }

  @Test
  @StdIo
  void shouldLogInfoAndAboveRegular(StdOut out) {
    LogConfigurer.configure(false);
    LOG.trace("trace shouldLogInfoAndAbove");
    LOG.debug("debug shouldLogInfoAndAbove");
    LOG.info("info shouldLogInfoAndAbove");
    LOG.warn("warn shouldLogInfoAndAbove");
    LOG.error("error shouldLogInfoAndAbove");
    await()
        .untilAsserted(
            () ->
                assertThat(StringUtils.join(out.capturedLines(), "\n"))
                    .contains("error shouldLogInfoAndAbove")
                    .contains("warn shouldLogInfoAndAbove")
                    .contains("info shouldLogInfoAndAbove")
                    .doesNotContain("debug shouldLogInfoAndAbove")
                    .doesNotContain("trace shouldLogInfoAndAbove"));
  }

  static class TestException extends RuntimeException {
    public TestException(String message) {
      super(message);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
      StackTraceElement stackTraceElement = super.getStackTrace()[0];
      return new StackTraceElement[] {
        new StackTraceElement(
            stackTraceElement.getClassLoaderName(),
            stackTraceElement.getModuleName(),
            stackTraceElement.getModuleVersion(),
            stackTraceElement.getClassName(),
            stackTraceElement.getMethodName(),
            stackTraceElement.getFileName(),
            100)
      };
    }
  }
}
