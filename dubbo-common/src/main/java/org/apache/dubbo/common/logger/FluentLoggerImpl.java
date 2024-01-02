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
package org.apache.dubbo.common.logger;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.helpers.FormattingTuple;
import org.apache.dubbo.common.logger.helpers.MessageFormatter;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.function.Supplier;

final class FluentLoggerImpl implements FluentLogger {

    private final ErrorTypeAwareLogger delegate;
    private final FluentLoggerImpl root;

    private String code;
    private String cause = StringUtils.EMPTY_STRING;
    private String extendedInformation = StringUtils.EMPTY_STRING;
    private Supplier<String> messageSupplier;
    private String message;
    private Object[] args;

    FluentLoggerImpl(Class<?> key) {
        delegate = LoggerFactory.getErrorTypeAwareLogger(key);
        root = this;
    }

    FluentLoggerImpl(String key) {
        delegate = LoggerFactory.getErrorTypeAwareLogger(key);
        root = this;
    }

    private FluentLoggerImpl(FluentLoggerImpl logger) {
        delegate = logger.delegate;
        root = logger;
    }

    @Override
    public FluentLogger code(String code) {
        FluentLoggerImpl logger = getLogger();
        logger.code = code;
        return logger;
    }

    @Override
    public FluentLogger cause(String cause) {
        if (cause == null) {
            return this;
        }
        FluentLoggerImpl logger = getLogger();
        logger.cause = cause;
        return logger;
    }

    @Override
    public FluentLogger more(String extendedInformation) {
        if (extendedInformation == null) {
            return this;
        }
        FluentLoggerImpl logger = getLogger();
        logger.extendedInformation = extendedInformation;
        return logger;
    }

    @Override
    public FluentLogger msg(String message) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        return logger;
    }

    @Override
    public FluentLogger msg(String message, Object... args) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.args = args;
        return logger;
    }

    @Override
    public FluentLogger msg(Supplier<String> supplier) {
        FluentLoggerImpl logger = getLogger();
        logger.messageSupplier = supplier;
        return logger;
    }

    @Override
    public FluentLogger unexpected(String message) {
        return code(LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION).msg(message);
    }

    @Override
    public FluentLogger unexpected(String message, Object... args) {
        return code(LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION).msg(message, args);
    }

    @Override
    public FluentLogger unexpected(Supplier<String> supplier) {
        return code(LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION).msg(supplier);
    }

    @Override
    public FluentLogger unknown(String message) {
        return code(LoggerCodeConstants.INTERNAL_ERROR).msg(message);
    }

    @Override
    public FluentLogger unknown(String message, Object... args) {
        return code(LoggerCodeConstants.INTERNAL_ERROR).msg(message, args);
    }

    @Override
    public FluentLogger unknown(Supplier<String> supplier) {
        return code(LoggerCodeConstants.INTERNAL_ERROR).msg(supplier);
    }

    @Override
    public void trace() {
        if (message != null) {
            if (args != null && args.length > 0) {
                delegate.trace(message, args);
            } else {
                delegate.trace(message);
            }
        } else if (messageSupplier != null && delegate.isTraceEnabled()) {
            delegate.trace(messageSupplier.get());
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    @Override
    public void trace(Throwable t) {
        delegate.trace(t);
    }

    @Override
    public void trace(String message) {
        delegate.trace(message);
    }

    @Override
    public void trace(String message, Object... args) {
        delegate.trace(message, args);
    }

    @Override
    public void trace(String message, Throwable t) {
        delegate.trace(message, t);
    }

    @Override
    public void debug() {
        if (message != null) {
            if (args != null && args.length > 0) {
                delegate.debug(message, args);
            } else {
                delegate.debug(message);
            }
        } else if (messageSupplier != null && delegate.isDebugEnabled()) {
            delegate.debug(messageSupplier.get());
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    @Override
    public void debug(Throwable t) {
        delegate.debug(t);
    }

    @Override
    public void debug(String message) {
        delegate.debug(message);
    }

    @Override
    public void debug(String message, Object... args) {
        delegate.debug(message, args);
    }

    @Override
    public void debug(String message, Throwable t) {
        delegate.debug(message, t);
    }

    @Override
    public void info() {
        if (message != null) {
            if (args != null && args.length > 0) {
                delegate.info(message, args);
            } else {
                delegate.info(message);
            }
        } else if (messageSupplier != null && delegate.isInfoEnabled()) {
            delegate.info(messageSupplier.get());
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    @Override
    public void info(Throwable t) {
        delegate.info(t);
    }

    @Override
    public void info(String message, Object... args) {
        delegate.info(message, args);
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void info(String message, Throwable t) {
        delegate.info(message, t);
    }

    @Override
    public void warn() {
        checkCode(code);
        if (message != null) {
            if (args != null && args.length > 0) {
                formatAndWarn(code, message, args);
            } else {
                delegate.warn(code, cause, extendedInformation, message);
            }
        } else if (messageSupplier != null && delegate.isWarnEnabled()) {
            delegate.warn(code, cause, extendedInformation, messageSupplier.get());
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    private void formatAndWarn(String code, String message, Object... args) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, args);
        if (tuple.getThrowable() == null) {
            delegate.warn(code, cause, extendedInformation, tuple.getMessage());
        } else {
            delegate.warn(code, cause, extendedInformation, tuple.getMessage(), tuple.getThrowable());
        }
    }

    @Override
    public void warn(Throwable t) {
        checkCode(code);
        if (message != null) {
            if (args != null && args.length > 0) {
                FormattingTuple tuple = MessageFormatter.arrayFormat(message, args);
                delegate.warn(code, cause, extendedInformation, tuple.getMessage(), t);
            } else {
                delegate.warn(code, cause, extendedInformation, message, t);
            }
        } else if (messageSupplier != null && delegate.isWarnEnabled()) {
            delegate.warn(code, cause, extendedInformation, messageSupplier.get(), t);
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    @Override
    public void warn(String message) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.warn();
    }

    @Override
    public void warn(String message, Object... args) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.args = args;
        logger.warn();
    }

    @Override
    public void warn(String message, Throwable t) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.warn(t);
    }

    @Override
    public void warn(String code, String message, Object... args) {
        checkCode(code);
        if (args == null || args.length == 0) {
            delegate.warn(code, cause, extendedInformation, message);
            return;
        }
        formatAndWarn(code, message, args);
    }

    @Override
    public void warn(String code, String message, Throwable t) {
        checkCode(code);
        delegate.warn(code, cause, extendedInformation, message, t);
    }

    @Override
    public void error() {
        checkCode(code);
        if (message != null) {
            if (args != null && args.length > 0) {
                formatAndError(code, message, args);
            } else {
                delegate.error(code, cause, extendedInformation, message);
            }
        } else if (messageSupplier != null && delegate.isErrorEnabled()) {
            delegate.error(code, cause, extendedInformation, messageSupplier.get());
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    private void formatAndError(String code, String message, Object... args) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, args);
        if (tuple.getThrowable() == null) {
            delegate.error(code, cause, extendedInformation, tuple.getMessage());
        } else {
            delegate.error(code, cause, extendedInformation, tuple.getMessage(), tuple.getThrowable());
        }
    }

    @Override
    public void error(Throwable t) {
        checkCode(code);
        if (message != null) {
            if (args != null && args.length > 0) {
                FormattingTuple tuple = MessageFormatter.arrayFormat(message, args);
                delegate.error(code, cause, extendedInformation, tuple.getMessage(), t);
            } else {
                delegate.error(code, cause, extendedInformation, message, t);
            }
        } else if (messageSupplier != null && delegate.isErrorEnabled()) {
            delegate.error(code, cause, extendedInformation, messageSupplier.get(), t);
        } else {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    @Override
    public void error(String message) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.error();
    }

    @Override
    public void error(String message, Object... args) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.args = args;
        logger.error();
    }

    @Override
    public void error(String message, Throwable t) {
        FluentLoggerImpl logger = getLogger();
        logger.message = message;
        logger.error(t);
    }

    @Override
    public void error(String code, String message, Object... args) {
        checkCode(code);
        if (args == null || args.length == 0) {
            delegate.error(code, cause, extendedInformation, message);
            return;
        }
        formatAndError(code, message, args);
    }

    @Override
    public void error(String code, String message, Throwable t) {
        checkCode(code);
        delegate.error(code, cause, extendedInformation, message, t);
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    private FluentLoggerImpl getLogger() {
        return this == root ? new FluentLoggerImpl(this) : this;
    }

    private static void checkCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Error code must not be null");
        }
    }
}
