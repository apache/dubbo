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
package org.apache.dubbo.remoting.transport.netty;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.jboss.netty.logging.AbstractInternalLogger;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

final class NettyHelper {

    public static void setNettyLoggerFactory() {
        InternalLoggerFactory factory = InternalLoggerFactory.getDefaultFactory();
        if (!(factory instanceof DubboLoggerFactory)) {
            InternalLoggerFactory.setDefaultFactory(new DubboLoggerFactory());
        }
    }

    static class DubboLoggerFactory extends InternalLoggerFactory {

        @Override
        public InternalLogger newInstance(String name) {
            return new DubboLogger(LoggerFactory.getErrorTypeAwareLogger(name));
        }
    }

    static class DubboLogger extends AbstractInternalLogger {

        public static final String LOGGER_CAUSE_STRING = "unknown error in remoting-netty module";
        private ErrorTypeAwareLogger logger;

        DubboLogger(ErrorTypeAwareLogger logger) {
            this.logger = logger;
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

        @Override
        public void debug(String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(String msg, Throwable cause) {
            logger.debug(msg, cause);
        }

        @Override
        public void info(String msg) {
            logger.info(msg);
        }

        @Override
        public void info(String msg, Throwable cause) {
            logger.info(msg, cause);
        }

        @Override
        public void warn(String msg) {
            logger.warn(INTERNAL_ERROR, LOGGER_CAUSE_STRING, "", msg);
        }

        @Override
        public void warn(String msg, Throwable cause) {
            logger.warn(INTERNAL_ERROR, LOGGER_CAUSE_STRING, "", msg, cause);
        }

        @Override
        public void error(String msg) {
            logger.error(INTERNAL_ERROR, LOGGER_CAUSE_STRING, "", msg);
        }

        @Override
        public void error(String msg, Throwable cause) {
            logger.error(INTERNAL_ERROR, LOGGER_CAUSE_STRING, "", msg, cause);
        }

        @Override
        public String toString() {
            return logger.toString();
        }
    }

}
