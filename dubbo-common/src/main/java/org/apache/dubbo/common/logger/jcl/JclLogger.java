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
package org.apache.dubbo.common.logger.jcl;

import org.apache.dubbo.common.logger.Logger;

import org.apache.commons.logging.Log;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

/**
 * Adaptor to commons logging, depends on commons-logging.jar. For more information about commons logging, pls. refer to
 * <a target="_blank" href="http://www.apache.org/">http://www.apache.org/</a>
 */
public class JclLogger implements Logger {

    private String fqcn;

    private final Log logger;

    public JclLogger(Log logger) {
        this.logger = logger;
    }

    public JclLogger(String fqcn, Log logger) {
        this.fqcn = fqcn;
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, null);
        } else {
            logger.trace(msg);
        }
    }

    @Override
    public void trace(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.TRACE_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.trace(ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void trace(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.TRACE_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.trace(e);
        }
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, e);
        } else {
            logger.trace(msg, e);
        }
    }

    @Override
    public void debug(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, null);
        } else {
            logger.debug(msg);
        }
    }

    @Override
    public void debug(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.DEBUG_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void debug(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.DEBUG_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.debug(e);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, e);
        } else {
            logger.debug(msg, e);
        }
    }

    @Override
    public void info(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, null);
        } else {
            logger.info(msg);
        }
    }

    @Override
    public void info(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.INFO_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.info(ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void info(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.INFO_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.info(e);
        }
    }

    @Override
    public void info(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, e);
        } else {
            logger.info(msg, e);
        }
    }

    @Override
    public void warn(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, null);
        } else {
            logger.warn(msg);
        }
    }

    @Override
    public void warn(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.WARN_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void warn(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.WARN_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.warn(e);
        }
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, e);
        } else {
            logger.warn(msg, e);
        }
    }

    @Override
    public void error(String msg) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, null);
        } else {
            logger.error(msg);
        }
    }

    @Override
    public void error(String msg, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.ERROR_INT, ft.getMessage(), null, ft.getThrowable());
        } else {
            logger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    @Override
    public void error(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.ERROR_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.error(e);
        }
    }

    @Override
    public void error(String msg, Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, e);
        } else {
            logger.error(msg, e);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }
}
