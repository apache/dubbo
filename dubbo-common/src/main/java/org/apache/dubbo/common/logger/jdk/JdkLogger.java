/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.logger.jdk;

import org.apache.dubbo.common.logger.Logger;

import java.util.logging.Level;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

public class JdkLogger implements Logger {

    private String fqcn;

    private final java.util.logging.Logger logger;

    public JdkLogger(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public JdkLogger(String fqcn, java.util.logging.Logger logger) {
        this.fqcn = fqcn;
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, null);
        } else {
            logger.log(Level.FINER, msg);
        }
    }

    @Override
    public void trace(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.TRACE_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.log(Level.FINER, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void trace(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.TRACE_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.log(Level.FINER, e.getMessage(), e);
        }
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, e);
        } else {
            logger.log(Level.FINER, msg, e);
        }
    }

    @Override
    public void debug(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, null);
        } else {
            logger.log(Level.FINE, msg);
        }
    }

    @Override
    public void debug(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.DEBUG_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.log(Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void debug(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.DEBUG_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.log(Level.FINE, e.getMessage(), e);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, e);
        } else {
            logger.log(Level.FINE, msg, e);
        }
    }

    @Override
    public void info(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, null);
        } else {
            logger.log(Level.INFO, msg);
        }
    }

    @Override
    public void info(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.INFO_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.log(Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void info(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, e);
        } else {
            logger.log(Level.INFO, msg, e);
        }
    }

    @Override
    public void warn(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, null);
        } else {
            logger.log(Level.WARNING, msg);
        }
    }

    @Override
    public void warn(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.WARN_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.log(Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, e);
        } else {
            logger.log(Level.WARNING, msg, e);
        }
    }

    @Override
    public void error(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, null);
        } else {
            logger.log(Level.SEVERE, msg);
        }
    }

    @Override
    public void error(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.ERROR_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.log(Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void error(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, e);
        } else {
            logger.log(Level.SEVERE, msg, e);
        }
    }

    @Override
    public void error(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.ERROR_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void info(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.INFO_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.log(Level.INFO, e.getMessage(), e);
        }
    }

    @Override
    public void warn(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.WARN_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }
}
