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
import org.apache.dubbo.common.logger.support.FailsafeLogger;

import org.apache.logging.log4j.Level;

public class Log4j2Logger implements Logger {

    private final String fqcn;

    private final org.apache.logging.log4j.Logger logger;

    public Log4j2Logger(org.apache.logging.log4j.Logger logger) {
        this.fqcn = FailsafeLogger.class.getName();
        this.logger = logger;
    }

    public Log4j2Logger(String fqcn, org.apache.logging.log4j.Logger logger) {
        this.fqcn = fqcn;
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        logger.logMessage(
                Level.TRACE, null, fqcn, null, logger.getMessageFactory().newMessage(msg), null);
    }

    @Override
    public void trace(String msg, Object... arguments) {
        logger.logMessage(
                Level.TRACE, null, fqcn, null, logger.getMessageFactory().newMessage(msg, arguments), null);
    }

    @Override
    public void trace(Throwable e) {
        logger.logMessage(
                Level.TRACE,
                null,
                fqcn,
                null,
                logger.getMessageFactory().newMessage(e == null ? null : e.getMessage()),
                e);
    }

    @Override
    public void trace(String msg, Throwable e) {
        logger.logMessage(
                Level.TRACE, null, fqcn, null, logger.getMessageFactory().newMessage(msg), e);
    }

    @Override
    public void debug(String msg) {
        logger.logMessage(
                Level.DEBUG, null, fqcn, null, logger.getMessageFactory().newMessage(msg), null);
    }

    @Override
    public void debug(String msg, Object... arguments) {
        logger.logMessage(
                Level.DEBUG, null, fqcn, null, logger.getMessageFactory().newMessage(msg, arguments), null);
    }

    @Override
    public void debug(Throwable e) {
        logger.logMessage(
                Level.DEBUG,
                null,
                fqcn,
                null,
                logger.getMessageFactory().newMessage(e == null ? null : e.getMessage()),
                e);
    }

    @Override
    public void debug(String msg, Throwable e) {
        logger.logMessage(
                Level.DEBUG, null, fqcn, null, logger.getMessageFactory().newMessage(msg), e);
    }

    @Override
    public void info(String msg) {
        logger.logMessage(
                Level.INFO, null, fqcn, null, logger.getMessageFactory().newMessage(msg), null);
    }

    @Override
    public void info(String msg, Object... arguments) {
        logger.logMessage(
                Level.INFO, null, fqcn, null, logger.getMessageFactory().newMessage(msg, arguments), null);
    }

    @Override
    public void info(Throwable e) {
        logger.logMessage(
                Level.INFO,
                null,
                fqcn,
                null,
                logger.getMessageFactory().newMessage(e == null ? null : e.getMessage()),
                e);
    }

    @Override
    public void info(String msg, Throwable e) {
        logger.logMessage(
                Level.INFO, null, fqcn, null, logger.getMessageFactory().newMessage(msg), e);
    }

    @Override
    public void warn(String msg) {
        logger.logMessage(
                Level.WARN, null, fqcn, null, logger.getMessageFactory().newMessage(msg), null);
    }

    @Override
    public void warn(String msg, Object... arguments) {
        logger.logMessage(
                Level.WARN, null, fqcn, null, logger.getMessageFactory().newMessage(msg, arguments), null);
    }

    @Override
    public void warn(Throwable e) {
        logger.logMessage(
                Level.WARN,
                null,
                fqcn,
                null,
                logger.getMessageFactory().newMessage(e == null ? null : e.getMessage()),
                e);
    }

    @Override
    public void warn(String msg, Throwable e) {
        logger.logMessage(
                Level.WARN, null, fqcn, null, logger.getMessageFactory().newMessage(msg), e);
    }

    @Override
    public void error(String msg) {
        logger.logMessage(
                Level.ERROR, null, fqcn, null, logger.getMessageFactory().newMessage(msg), null);
    }

    @Override
    public void error(String msg, Object... arguments) {
        logger.logMessage(
                Level.ERROR, null, fqcn, null, logger.getMessageFactory().newMessage(msg, arguments), null);
    }

    @Override
    public void error(Throwable e) {
        logger.logMessage(
                Level.ERROR,
                null,
                fqcn,
                null,
                logger.getMessageFactory().newMessage(e == null ? null : e.getMessage()),
                e);
    }

    @Override
    public void error(String msg, Throwable e) {
        logger.logMessage(
                Level.ERROR, null, fqcn, null, logger.getMessageFactory().newMessage(msg), e);
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
