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

/**
 * Logger interface with the ability of displaying solution of different types of error.
 */
public interface ErrorTypeAwareLogger extends Logger {

    /**
     * Logs a message with trace log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     */
    void trace(ErrorType errorType, String msg);

    /**
     * Logs a message with trace log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     * @param e log this cause
     */
    void trace(ErrorType errorType, String msg, Throwable e);

    /**
     * Logs a message with debug log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     */
    void debug(ErrorType errorType, String msg);

    /**
     * Logs a message with debug log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     * @param e log this cause
     */
    void debug(ErrorType errorType, String msg, Throwable e);

    /**
     * Logs a message with info log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     */
    void info(ErrorType errorType, String msg);

    /**
     * Logs a message with info log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     * @param e log this cause
     */
    void info(ErrorType errorType, String msg, Throwable e);

    /**
     * Logs a message with warn log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     */
    void warn(ErrorType errorType, String msg);

    /**
     * Logs a message with warn log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     * @param e log this cause
     */
    void warn(ErrorType errorType, String msg, Throwable e);

    /**
     * Logs a message with error log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     */
    void error(ErrorType errorType, String msg);

    /**
     * Logs a message with error log level.
     *
     * @param errorType error type of this log
     * @param msg log this message
     * @param e log this cause
     */
    void error(ErrorType errorType, String msg, Throwable e);
}
