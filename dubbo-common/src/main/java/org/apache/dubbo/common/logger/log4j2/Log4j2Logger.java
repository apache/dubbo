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
package org.apache.dubbo.common.logger.log4j2;

import org.apache.dubbo.common.logger.Logger;

import org.slf4j.spi.LocationAwareLogger;

public class Log4j2Logger implements Logger {

    private String fqcn;

    private final org.apache.logging.log4j.Logger logger;

    public Log4j2Logger(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    public Log4j2Logger(String fqcn, org.apache.logging.log4j.Logger logger) {
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
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, arguments, null);
        } else {
            logger.trace(msg, arguments);
        }
    }

    @Override
    public void trace(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.TRACE_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.trace(e == null ? null : e.getMessage(), e);
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
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, arguments, null);
        } else {
            logger.debug(msg, arguments);
        }
    }

    @Override
    public void debug(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.DEBUG_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.debug(e == null ? null : e.getMessage(), e);
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
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.INFO_INT, msg, arguments, null);
        } else {
            logger.info(msg, arguments);
        }
    }

    @Override
    public void info(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.INFO_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.info(e == null ? null : e.getMessage(), e);
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
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.WARN_INT, msg, arguments, null);
        } else {
            logger.warn(msg, arguments);
        }
    }

    @Override
    public void warn(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.WARN_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.warn(e == null ? null : e.getMessage(), e);
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
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger).log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, arguments, null);
        } else {
            logger.error(msg, arguments);
        }
    }

    @Override
    public void error(Throwable e) {
        if (fqcn != null && logger instanceof LocationAwareLogger) {
            ((LocationAwareLogger) logger)
                    .log(null, fqcn, LocationAwareLogger.ERROR_INT, e == null ? null : e.getMessage(), null, e);
        } else {
            logger.error(e == null ? null : e.getMessage(), e);
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

    // test purpose only
    public org.apache.logging.log4j.Logger getLogger() {
        return logger;
    }
}
