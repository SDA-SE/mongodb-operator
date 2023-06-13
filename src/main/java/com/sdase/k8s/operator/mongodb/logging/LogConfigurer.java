package com.sdase.k8s.operator.mongodb.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogConfigurer {
  private LogConfigurer() {
    // utility
  }

  public static void configure(boolean enableJson) {
    if (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        instanceof ch.qos.logback.classic.Logger rootLogger) {
      var loggerContext = rootLogger.getLoggerContext();
      rootLogger.detachAndStopAllAppenders();
      if (enableJson) {
        rootLogger.addAppender(jsonAppender(loggerContext));
      } else {
        rootLogger.addAppender(defaultAppender(loggerContext));
      }
      rootLogger.setLevel(Level.INFO);
    }
  }

  private static Appender<ILoggingEvent> defaultAppender(LoggerContext loggerContext) {
    var layout = new TTLLLayout();
    layout.setContext(loggerContext);
    var appender = new ConsoleAppender<ILoggingEvent>();
    appender.setName("console");
    appender.setContext(loggerContext);
    appender.setLayout(layout);
    layout.start();
    appender.start();
    return appender;
  }

  private static Appender<ILoggingEvent> jsonAppender(LoggerContext loggerContext) {
    var jsonLayout = new JsonLayout();
    jsonLayout.setContext(loggerContext);
    jsonLayout.setJsonFormatter(new JacksonJsonFormatter());
    jsonLayout.setAppendLineSeparator(true);
    jsonLayout.setIncludeException(true);
    var encoder = new LayoutWrappingEncoder<ILoggingEvent>();
    encoder.setContext(loggerContext);
    encoder.setLayout(jsonLayout);
    var appender = new ConsoleAppender<ILoggingEvent>();
    appender.setName("console");
    appender.setContext(loggerContext);
    appender.setEncoder(encoder);
    appender.setLayout(jsonLayout);

    jsonLayout.start();
    encoder.start();
    appender.start();

    return appender;
  }
}
