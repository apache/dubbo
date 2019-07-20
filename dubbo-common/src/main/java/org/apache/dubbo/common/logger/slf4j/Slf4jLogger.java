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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;

import org.slf4j.spi.LocationAwareLogger;

import java.io.Serializable;

public class Slf4jLogger implements Logger, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FQCN = FailsafeLogger.class.getName();

    private final org.slf4j.Logger logger;

    private final LocationAwareLogger locationAwareLogger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        if (logger instanceof LocationAwareLogger) {
            locationAwareLogger = (LocationAwareLogger) logger;
        } else {
            locationAwareLogger = null;
        }
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, msg, null, null);
            return;
        }
        logger.trace(msg);
    }

    @Override
    public void trace(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, e.getMessage(), null, e);
            return;
        }
        logger.trace(e.getMessage(), e);
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, msg, null, e);
            return;
        }
        logger.trace(msg, e);
    }

    @Override
    public void debug(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null, null);
            return;
        }
        logger.debug(msg);
    }

    @Override
    public void debug(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, e.getMessage(), null, e);
            return;
        }
        logger.debug(e.getMessage(), e);
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null, e);
            return;
        }
        logger.debug(msg, e);
    }

    @Override
    public void info(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null, null);
            return;
        }
        logger.info(msg);
    }

    @Override
    public void info(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, e.getMessage(), null, e);
            return;
        }
        logger.info(e.getMessage(), e);
    }

    @Override
    public void info(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null, e);
            return;
        }
        logger.info(msg, e);
    }

    @Override
    public void warn(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null, null);
            return;
        }
        logger.warn(msg);
    }

    @Override
    public void warn(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, e.getMessage(), null, e);
            return;
        }
        logger.warn(e.getMessage(), e);
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null, e);
            return;
        }
        logger.warn(msg, e);
    }

    @Override
    public void error(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null, null);
            return;
        }
        logger.error(msg);
    }

    @Override
    public void error(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, e.getMessage(), null, e);
            return;
        }
        logger.error(e.getMessage(), e);
    }

    @Override
    public void error(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null, e);
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

}
