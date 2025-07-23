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

import java.util.function.Supplier;

public interface FluentLogger {

    FluentLogger cause(String cause);

    FluentLogger more(String extendedInformation);

    FluentLogger msg(String msg);

    FluentLogger msg(String msg, Object... args);

    FluentLogger msg(Supplier<String> supplier);

    void trace();

    void trace(Throwable t);

    void trace(String msg);

    void trace(String msg, Object... args);

    void trace(String msg, Throwable t);

    void debug();

    void debug(Throwable t);

    void debug(String msg);

    void debug(String msg, Object... args);

    void debug(String msg, Throwable t);

    void info();

    void info(Throwable t);

    void info(String msg, Object... args);

    void info(String msg);

    void info(String msg, Throwable t);

    void internalWarn();

    void internalWarn(Throwable t);

    void internalWarn(String msg);

    void internalWarn(String msg, Object... args);

    void internalWarn(String msg, Throwable t);

    void warn(String code);

    void warn(String code, Throwable t);

    void warn(String code, String msg, Object... args);

    void warn(String code, String msg, Throwable t);

    void internalError();

    void internalError(Throwable t);

    void internalError(String msg);

    void internalError(String msg, Object... args);

    void internalError(String msg, Throwable t);

    void error(String code);

    void error(String code, Throwable t);

    void error(String code, String msg, Object... args);

    void error(String code, String msg, Throwable t);

    void log(Level level);

    void log(Level level, Throwable t);

    void log(Level level, String msg);

    void log(Level level, String msg, Object... args);

    void log(Level level, String msg, Throwable t);

    void log(String code, Level level);

    void log(String code, Level level, String msg, Object... args);

    void log(String code, Level level, String msg, Throwable t);

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    static FluentLogger of(Class<?> key) {
        return new FluentLoggerImpl(key);
    }

    static FluentLogger of(String key) {
        return new FluentLoggerImpl(key);
    }

    interface S extends Supplier<String> {}
}
