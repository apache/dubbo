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
package org.apache.dubbo.remoting.http.jetty;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;

/**
 * logger adapter for jetty
 */
public class JettyLoggerAdapter extends AbstractLogger {
    protected String name;

    private final ErrorTypeAwareLogger logger;

    private static boolean debugEnabled = false;

    public JettyLoggerAdapter() {
        this("org.apache.dubbo.remoting.http.jetty");
    }

    public JettyLoggerAdapter(Class<?> clazz) {
        this(clazz.getName());
    }

    public JettyLoggerAdapter(String name) {
        this.name = name;
        this.logger = LoggerFactory.getErrorTypeAwareLogger(name);
    }

    @Override
    protected Logger newLogger(String name) {
        return new JettyLoggerAdapter(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void warn(String msg, Object... objects) {
        if (logger.isWarnEnabled()) {
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", this.format(msg, objects));
        }
    }

    @Override
    public void warn(Throwable throwable) {
        if (logger.isWarnEnabled()) {
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", throwable.getMessage(), throwable);
        }
    }

    @Override
    public void warn(String msg, Throwable throwable) {
        if (logger.isWarnEnabled()) {
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", msg, throwable);
        }
    }

    @Override
    public void info(String msg, Object... objects) {
        if (logger.isInfoEnabled()) {
            logger.info(this.format(msg, objects));
        }
    }

    @Override
    public void info(Throwable throwable) {
        if (logger.isInfoEnabled()) {
            logger.info(throwable);
        }
    }

    @Override
    public void info(String msg, Throwable throwable) {
        if (logger.isInfoEnabled()) {
            logger.info(msg, throwable);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    @Override
    public void debug(String msg, Object... objects) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(this.format(msg, objects));
        }
    }

    @Override
    public void debug(Throwable throwable) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(throwable);
        }
    }

    @Override
    public void debug(String msg, Throwable throwable) {
        if (debugEnabled && logger.isDebugEnabled()) {
            logger.debug(msg, throwable);
        }
    }

    @Override
    public void ignore(Throwable throwable) {
        if (logger.isWarnEnabled()) {
            logger.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", "IGNORED EXCEPTION ", throwable);
        }
    }

    private String format(String msg, Object... args) {
        msg = String.valueOf(msg); // Avoids NPE
        String braces = "{}";
        StringBuilder builder = new StringBuilder();
        int start = 0;
        for (Object arg : args) {
            int bracesIndex = msg.indexOf(braces, start);
            if (bracesIndex < 0) {
                builder.append(msg.substring(start));
                builder.append(' ');
                builder.append(arg);
                start = msg.length();
            } else {
                builder.append(msg, start, bracesIndex);
                builder.append(arg);
                start = bracesIndex + braces.length();
            }
        }
        builder.append(msg.substring(start));
        return builder.toString();
    }
}
