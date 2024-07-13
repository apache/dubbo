/*
 * Copyright 2020 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import io.grpc.Internal;
import io.grpc.InternalLogId;

/**
 * An xDS-specific logger for collecting xDS specific events. Information logged here goes
 * to the Java logger of this class.
 */
@Internal
public final class XdsLogger {
  private static final Logger logger = Logger.getLogger("org.apache.dubbo.xds.XdsLogger");

  private final String prefix;

  public static XdsLogger withLogId(InternalLogId logId) {
    Preconditions.checkNotNull(logId, "logId");
    return new XdsLogger(logId.toString());
  }

  static XdsLogger withPrefix(String prefix) {
    return new XdsLogger(prefix);
  }

  private XdsLogger(String prefix) {
    this.prefix = Preconditions.checkNotNull(prefix, "prefix");
  }

  public boolean isLoggable(XdsLogLevel level) {
    Level javaLevel = toJavaLogLevel(level);
    return logger.isLoggable(javaLevel);
  }

  void log(XdsLogLevel level, String msg) {
    Level javaLevel = toJavaLogLevel(level);
    logOnly(prefix, javaLevel, msg);
  }

  public void log(XdsLogLevel level, String messageFormat, Object... args) {
    Level javaLogLevel = toJavaLogLevel(level);
    if (logger.isLoggable(javaLogLevel)) {
      String msg = MessageFormat.format(messageFormat, args);
      logOnly(prefix, javaLogLevel, msg);
    }
  }

  private static void logOnly(String prefix, Level logLevel, String msg) {
    if (logger.isLoggable(logLevel)) {
      LogRecord lr = new LogRecord(logLevel, "[" + prefix + "] " + msg);
      // No resource bundle as gRPC is not localized.
      lr.setLoggerName(logger.getName());
      lr.setSourceClassName(logger.getName());
      lr.setSourceMethodName("log");
      logger.log(lr);
    }
  }

  private static Level toJavaLogLevel(XdsLogLevel level) {
    switch (level) {
      case ERROR:
      case WARNING:
        return Level.FINE;
      case INFO:
        return Level.FINER;
      case FORCE_INFO:
        return Level.INFO;
      case FORCE_WARNING:
        return Level.WARNING;
      default:
        return Level.FINEST;
    }
  }

  /**
   * Log levels. See the table below for the mapping from the XdsLogger levels to
   * Java logger levels.
   *
   * <p><b>NOTE:</b>
   *   Please use {@code FORCE_} levels with care, only when the message is expected to be
   *   surfaced to the library user. Normally libraries should minimize the usage
   *   of highly visible logs.
   * <pre>
   * +---------------------+-------------------+
   * | XdsLogger Level     | Java Logger Level |
   * +---------------------+-------------------+
   * | DEBUG               | FINEST            |
   * | INFO                | FINER             |
   * | WARNING             | FINE              |
   * | ERROR               | FINE              |
   * | FORCE_INFO          | INFO              |
   * | FORCE_WARNING       | WARNING           |
   * +---------------------+-------------------+
   * </pre>
   */
  public enum XdsLogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FORCE_INFO,
    FORCE_WARNING,
  }
}
