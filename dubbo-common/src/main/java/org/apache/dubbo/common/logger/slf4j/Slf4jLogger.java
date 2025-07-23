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
package org.apache.dubbo.common.logger.slf4j;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

public class Slf4jLogger implements Logger {

    private final String fqcn;

    private final org.slf4j.Logger logger;

    private final LocationAwareLogger locationAwareLogger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        if (logger instanceof LocationAwareLogger) {
            locationAwareLogger = (LocationAwareLogger) logger;
        } else {
            locationAwareLogger = null;
        }
        this.fqcn = FailsafeLogger.class.getName();
        this.logger = logger;
    }

    public Slf4jLogger(String fqcn, org.slf4j.Logger logger) {
        if (logger instanceof LocationAwareLogger) {
            locationAwareLogger = (LocationAwareLogger) logger;
        } else {
            locationAwareLogger = null;
        }
        this.fqcn = fqcn;
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, null);
            return;
        }
        logger.trace(msg);
    }

    @Override
    public void trace(String msg, Object... arguments) {
        if (locationAwareLogger != null && locationAwareLogger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.TRACE_INT, msg, ft.getArgArray(), ft.getThrowable());
            return;
        }
        logger.trace(msg, arguments);
    }

    @Override
    public void trace(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.TRACE_INT, e == null ? null : e.getMessage(), null, e);
            return;
        }
        logger.trace(e == null ? null : e.getMessage(), e);
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.TRACE_INT, msg, null, e);
            return;
        }
        logger.trace(msg, e);
    }

    @Override
    public void debug(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, null);
            return;
        }
        logger.debug(msg);
    }

    @Override
    public void debug(String msg, Object... arguments) {
        if (locationAwareLogger != null && locationAwareLogger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.DEBUG_INT, msg, ft.getArgArray(), ft.getThrowable());
            return;
        }
        logger.debug(msg, arguments);
    }

    @Override
    public void debug(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.DEBUG_INT, e == null ? null : e.getMessage(), null, e);
            return;
        }
        logger.debug(e == null ? null : e.getMessage(), e);
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.DEBUG_INT, msg, null, e);
            return;
        }
        logger.debug(msg, e);
    }

    @Override
    public void info(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, null);
            return;
        }
        logger.info(msg);
    }

    @Override
    public void info(String msg, Object... arguments) {
        if (locationAwareLogger != null && locationAwareLogger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.INFO_INT, msg, ft.getArgArray(), ft.getThrowable());
            return;
        }
        logger.info(msg, arguments);
    }

    @Override
    public void info(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.INFO_INT, e == null ? null : e.getMessage(), null, e);
            return;
        }
        logger.info(e == null ? null : e.getMessage(), e);
    }

    @Override
    public void info(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.INFO_INT, msg, null, e);
            return;
        }
        logger.info(msg, e);
    }

    @Override
    public void warn(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, null);
            return;
        }
        logger.warn(msg);
    }

    @Override
    public void warn(String msg, Object... arguments) {
        if (locationAwareLogger != null && locationAwareLogger.isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.WARN_INT, msg, ft.getArgArray(), ft.getThrowable());
            return;
        }
        logger.warn(msg, arguments);
    }

    @Override
    public void warn(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.WARN_INT, e == null ? null : e.getMessage(), null, e);
            return;
        }
        logger.warn(e == null ? null : e.getMessage(), e);
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.WARN_INT, msg, null, e);
            return;
        }
        logger.warn(msg, e);
    }

    @Override
    public void error(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, null);
            return;
        }
        logger.error(msg);
    }

    @Override
    public void error(String msg, Object... arguments) {
        if (locationAwareLogger != null && locationAwareLogger.isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(msg, arguments);
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.ERROR_INT, msg, ft.getArgArray(), ft.getThrowable());
            return;
        }
        logger.error(msg, arguments);
    }

    @Override
    public void error(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(
                    null, fqcn, LocationAwareLogger.ERROR_INT, e == null ? null : e.getMessage(), null, e);
            return;
        }
        logger.error(e == null ? null : e.getMessage(), e);
    }

    @Override
    public void error(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, fqcn, LocationAwareLogger.ERROR_INT, msg, null, e);
            return;
        }
        logger.error(msg, e);
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

    public static Level getLevel(org.slf4j.Logger logger) {
        if (logger.isTraceEnabled()) {
            return Level.TRACE;
        }
        if (logger.isDebugEnabled()) {
            return Level.DEBUG;
        }
        if (logger.isInfoEnabled()) {
            return Level.INFO;
        }
        if (logger.isWarnEnabled()) {
            return Level.WARN;
        }
        if (logger.isErrorEnabled()) {
            return Level.ERROR;
        }
        return Level.OFF;
    }

    public Level getLevel() {
        return getLevel(logger);
    }
}
