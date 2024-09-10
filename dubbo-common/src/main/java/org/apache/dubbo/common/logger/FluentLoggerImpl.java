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

import org.apache.dubbo.common.logger.helpers.FormattingTuple;
import org.apache.dubbo.common.logger.helpers.MessageFormatter;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

final class FluentLoggerImpl implements FluentLogger {

    private final ErrorTypeAwareLogger delegate;
    private final FluentLoggerImpl root;

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
    public void trace() {
        if (message != null) {
            if (args != null && args.length > 0) {
                if (delegate.isTraceEnabled()) {
                    delegate.trace(message, formatArgs(args));
                }
            } else {
                delegate.trace(message);
            }
        } else if (messageSupplier != null) {
            if (delegate.isTraceEnabled()) {
                delegate.trace(messageSupplier.get());
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void trace(Throwable t) {
        if (message != null) {
            int len = args == null ? 0 : args.length;
            if (len > 0) {
                if (delegate.isTraceEnabled()) {
                    Object[] arr = new Object[len + 1];
                    System.arraycopy(args, 0, arr, 0, len);
                    arr[len] = t;
                    delegate.trace(message, formatArgs(arr));
                }
            } else {
                delegate.trace(message, t);
            }
        } else if (messageSupplier != null) {
            if (delegate.isTraceEnabled()) {
                delegate.trace(messageSupplier.get(), t);
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void trace(String message) {
        delegate.trace(message);
    }

    @Override
    public void trace(String message, Object... args) {
        if (args == null || args.length == 0) {
            delegate.trace(message);
        } else if (delegate.isTraceEnabled()) {
            delegate.trace(message, formatArgs(args));
        }
    }

    @Override
    public void trace(String message, Throwable t) {
        delegate.trace(message, t);
    }

    @Override
    public void debug() {
        if (message != null) {
            if (args != null && args.length > 0) {
                if (delegate.isDebugEnabled()) {
                    delegate.debug(message, formatArgs(args));
                }
            } else {
                delegate.debug(message);
            }
        } else if (messageSupplier != null) {
            if (delegate.isDebugEnabled()) {
                delegate.debug(messageSupplier.get());
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void debug(Throwable t) {
        if (message != null) {
            int len = args == null ? 0 : args.length;
            if (len > 0) {
                if (delegate.isDebugEnabled()) {
                    Object[] arr = new Object[len + 1];
                    System.arraycopy(args, 0, arr, 0, len);
                    arr[len] = t;
                    delegate.debug(message, formatArgs(arr));
                }
            } else {
                delegate.debug(message, t);
            }
        } else if (messageSupplier != null) {
            if (delegate.isDebugEnabled()) {
                delegate.debug(messageSupplier.get(), t);
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void debug(String message) {
        delegate.debug(message);
    }

    @Override
    public void debug(String message, Object... args) {
        if (args == null || args.length == 0) {
            delegate.debug(message);
        } else if (delegate.isDebugEnabled()) {
            delegate.debug(message, formatArgs(args));
        }
    }

    @Override
    public void debug(String message, Throwable t) {
        delegate.debug(message, t);
    }

    @Override
    public void info() {
        if (message != null) {
            if (args != null && args.length > 0) {
                if (delegate.isInfoEnabled()) {
                    delegate.info(message, formatArgs(args));
                }
            } else {
                delegate.info(message);
            }
        } else if (messageSupplier != null) {
            if (delegate.isInfoEnabled()) {
                delegate.info(messageSupplier.get());
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void info(Throwable t) {
        if (message != null) {
            int len = args == null ? 0 : args.length;
            if (len > 0) {
                if (delegate.isInfoEnabled()) {
                    Object[] arr = new Object[len + 1];
                    System.arraycopy(args, 0, arr, 0, len);
                    arr[len] = t;
                    delegate.info(message, formatArgs(arr));
                }
            } else {
                delegate.info(message, t);
            }
        } else if (messageSupplier != null) {
            if (delegate.isInfoEnabled()) {
                delegate.info(messageSupplier.get(), t);
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void info(String message, Object... args) {
        if (args == null || args.length == 0) {
            delegate.info(message);
        } else if (delegate.isInfoEnabled()) {
            delegate.info(message, formatArgs(args));
        }
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
    public void internalWarn() {
        warn(INTERNAL_ERROR);
    }

    @Override
    public void internalWarn(Throwable t) {
        warn(INTERNAL_ERROR, t);
    }

    @Override
    public void internalWarn(String message) {
        warn(INTERNAL_ERROR, message);
    }

    @Override
    public void internalWarn(String message, Object... args) {
        warn(INTERNAL_ERROR, message, args);
    }

    @Override
    public void internalWarn(String message, Throwable t) {
        warn(INTERNAL_ERROR, message, t);
    }

    @Override
    public void warn(String code) {
        if (message != null) {
            if (args != null && args.length > 0) {
                formatAndWarn(code, message, args);
            } else {
                delegate.warn(code, cause, extendedInformation, message);
            }
        } else if (messageSupplier != null) {
            if (delegate.isWarnEnabled()) {
                delegate.warn(code, cause, extendedInformation, messageSupplier.get());
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void warn(String code, Throwable t) {
        if (message != null) {
            if (args != null && args.length > 0) {
                if (delegate.isWarnEnabled()) {
                    FormattingTuple tuple = MessageFormatter.arrayFormat(message, formatArgs(args));
                    delegate.warn(code, cause, extendedInformation, tuple.getMessage(), t);
                }
            } else {
                delegate.warn(code, cause, extendedInformation, message, t);
            }
        } else if (messageSupplier != null && delegate.isWarnEnabled()) {
            delegate.warn(code, cause, extendedInformation, messageSupplier.get(), t);
        }
    }

    @Override
    public void warn(String code, String message, Object... args) {
        if (args == null || args.length == 0) {
            delegate.warn(code, cause, extendedInformation, message);
            return;
        }
        formatAndWarn(code, message, args);
    }

    private void formatAndWarn(String code, String message, Object... args) {
        if (!delegate.isWarnEnabled()) {
            return;
        }
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, formatArgs(args));
        if (tuple.getThrowable() == null) {
            delegate.warn(code, cause, extendedInformation, tuple.getMessage());
        } else {
            delegate.warn(code, cause, extendedInformation, tuple.getMessage(), tuple.getThrowable());
        }
    }

    @Override
    public void warn(String code, String message, Throwable t) {
        delegate.warn(code, cause, extendedInformation, message, t);
    }

    @Override
    public void internalError() {
        error(INTERNAL_ERROR);
    }

    @Override
    public void internalError(Throwable t) {
        error(INTERNAL_ERROR, t);
    }

    @Override
    public void internalError(String message) {
        error(INTERNAL_ERROR, message);
    }

    @Override
    public void internalError(String message, Object... args) {
        error(INTERNAL_ERROR, message, args);
    }

    @Override
    public void internalError(String message, Throwable t) {
        error(INTERNAL_ERROR, message, t);
    }

    @Override
    public void error(String code) {
        if (message != null) {
            if (args != null && args.length > 0) {
                formatAndError(code, message, args);
            } else {
                delegate.error(code, cause, extendedInformation, message);
            }
        } else if (messageSupplier != null) {
            if (delegate.isErrorEnabled()) {
                delegate.error(code, cause, extendedInformation, messageSupplier.get());
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void error(String code, Throwable t) {
        if (message != null) {
            if (args != null && args.length > 0) {
                if (delegate.isErrorEnabled()) {
                    FormattingTuple tuple = MessageFormatter.arrayFormat(message, formatArgs(args));
                    delegate.error(code, cause, extendedInformation, tuple.getMessage(), t);
                }
            } else {
                delegate.error(code, cause, extendedInformation, message, t);
            }
        } else if (messageSupplier != null) {
            if (delegate.isErrorEnabled()) {
                delegate.error(code, cause, extendedInformation, messageSupplier.get(), t);
            }
        } else {
            warnMessageMissing();
        }
    }

    @Override
    public void error(String code, String message, Object... args) {
        if (args == null || args.length == 0) {
            delegate.error(code, cause, extendedInformation, message);
            return;
        }
        formatAndError(code, message, args);
    }

    private void formatAndError(String code, String message, Object... args) {
        if (!delegate.isErrorEnabled()) {
            return;
        }
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, formatArgs(args));
        if (tuple.getThrowable() == null) {
            delegate.error(code, cause, extendedInformation, tuple.getMessage());
        } else {
            delegate.error(code, cause, extendedInformation, tuple.getMessage(), tuple.getThrowable());
        }
    }

    @Override
    public void error(String code, String message, Throwable t) {
        delegate.error(code, cause, extendedInformation, message, t);
    }

    @Override
    public void log(Level level) {
        switch (level) {
            case TRACE:
                trace();
                break;
            case DEBUG:
                debug();
                break;
            case INFO:
                info();
                break;
            case WARN:
                internalWarn();
                break;
            case ERROR:
                internalError();
                break;
            default:
        }
    }

    @Override
    public void log(Level level, Throwable t) {
        switch (level) {
            case TRACE:
                trace(t);
                break;
            case DEBUG:
                debug(t);
                break;
            case INFO:
                info(t);
                break;
            case WARN:
                internalWarn(t);
                break;
            case ERROR:
                internalError(t);
                break;
            default:
        }
    }

    @Override
    public void log(Level level, String msg) {
        switch (level) {
            case TRACE:
                trace(msg);
                break;
            case DEBUG:
                debug(msg);
                break;
            case INFO:
                info(msg);
                break;
            case WARN:
                internalWarn(msg);
                break;
            case ERROR:
                internalError(msg);
                break;
            default:
        }
    }

    @Override
    public void log(Level level, String msg, Object... args) {
        switch (level) {
            case TRACE:
                trace(msg, args);
                break;
            case DEBUG:
                debug(msg, args);
                break;
            case INFO:
                info(msg, args);
                break;
            case WARN:
                internalWarn(msg, args);
                break;
            case ERROR:
                internalError(msg, args);
                break;
            default:
        }
    }

    @Override
    public void log(Level level, String msg, Throwable t) {
        switch (level) {
            case TRACE:
                trace(msg, t);
                break;
            case DEBUG:
                debug(msg, t);
                break;
            case INFO:
                info(msg, t);
                break;
            case WARN:
                internalWarn(msg, t);
                break;
            case ERROR:
                internalError(msg, t);
                break;
            default:
        }
    }

    @Override
    public void log(String code, Level level) {
        switch (level) {
            case TRACE:
                trace();
                break;
            case DEBUG:
                debug();
                break;
            case INFO:
                info();
                break;
            case WARN:
                warn(code);
                break;
            case ERROR:
                error(code);
                break;
            default:
        }
    }

    @Override
    public void log(String code, Level level, String msg, Object... args) {
        switch (level) {
            case TRACE:
                trace(msg, args);
                break;
            case DEBUG:
                debug(msg, args);
                break;
            case INFO:
                info(msg, args);
                break;
            case WARN:
                warn(code, msg, args);
                break;
            case ERROR:
                error(code, msg, args);
                break;
            default:
        }
    }

    @Override
    public void log(String code, Level level, String msg, Throwable t) {
        switch (level) {
            case TRACE:
                trace(msg, t);
                break;
            case DEBUG:
                debug(msg, t);
                break;
            case INFO:
                info(msg, t);
                break;
            case WARN:
                warn(code, msg, t);
                break;
            case ERROR:
                error(code, msg, t);
                break;
            default:
        }
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

    private void warnMessageMissing() {
        delegate.warn(INTERNAL_ERROR, cause, extendedInformation, "Message must not be empty");
    }

    private static Object[] formatArgs(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Supplier) {
                args[i] = ((Supplier<?>) args[i]).get();
            }
        }
        return args;
    }
}
