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
package com.alibaba.dubbo.remoting.transport.netty4.logging;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class NettyHelper {

    public static void setNettyLoggerFactory() {
        InternalLoggerFactory factory = InternalLoggerFactory.getDefaultFactory();
        if (factory == null || !(factory instanceof DubboLoggerFactory)) {
            InternalLoggerFactory.setDefaultFactory(new DubboLoggerFactory());
        }
    }

    static class DubboLoggerFactory extends InternalLoggerFactory {

        @Override
        public InternalLogger newInstance(String name) {
            return new DubboLogger(LoggerFactory.getLogger(name));
        }
    }

    static class DubboLogger extends AbstractInternalLogger {

        private Logger logger;

        DubboLogger(Logger logger) {
            super(logger.getClass().getName());
            this.logger = logger;
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            if (isTraceEnabled()) {
                logger.trace(msg);
            }
        }

        @Override
        public void trace(String format, Object arg) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                logger.trace(ft.getMessage(), ft.getThrowable());
            }

        }

        @Override
        public void trace(String format, Object argA, Object argB) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                logger.trace(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (isTraceEnabled()) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
                logger.trace(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void trace(String msg, Throwable t) {
            if (isTraceEnabled()) {
                logger.trace(msg, t);
            }
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                logger.debug(msg);
            }
        }

        @Override
        public void debug(String format, Object arg) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                logger.debug(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String format, Object argA, Object argB) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                logger.debug(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (isDebugEnabled()) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
                logger.debug(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void debug(String msg, Throwable t) {
            if (isDebugEnabled()) {
                logger.debug(msg, t);
            }
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            if (isInfoEnabled()) {
                logger.info(msg);
            }
        }

        @Override
        public void info(String format, Object arg) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                logger.info(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String format, Object argA, Object argB) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                logger.info(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String format, Object... arguments) {
            if (isInfoEnabled()) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
                logger.info(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void info(String msg, Throwable t) {
            if (isInfoEnabled()) {
                logger.info(msg, t);
            }
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void warn(String msg) {
            if (isWarnEnabled()) {
                logger.warn(msg);
            }
        }

        @Override
        public void warn(String format, Object arg) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                logger.warn(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
                logger.warn(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String format, Object argA, Object argB) {
            if (isWarnEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                logger.warn(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void warn(String msg, Throwable t) {
            if (isWarnEnabled()) {
                logger.warn(msg, t);
            }
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public void error(String msg) {
            if (isErrorEnabled()) {
                logger.error(msg);
            }
        }

        @Override
        public void error(String format, Object arg) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                logger.error(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String format, Object argA, Object argB) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                logger.error(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String format, Object... arguments) {
            if (isErrorEnabled()) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
                logger.error(ft.getMessage(), ft.getThrowable());
            }
        }

        @Override
        public void error(String msg, Throwable t) {
            if (isErrorEnabled()) {
                logger.error(msg, t);
            }
        }
    }

}
