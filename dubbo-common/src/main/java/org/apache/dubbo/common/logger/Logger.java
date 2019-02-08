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
 * Logger interface
 * <p>
 * This interface is referred from commons-logging
 */
public interface Logger {

    /**
     * Logs a message with trace log level.
     *
     * @param msg log this message
     */
    void trace(String msg);
    
    /**
     * Logs a message with trace log level.
     * 
     * @param format    the format string
     * @param arguments arguments
     */
    default void trace(String format, Object... arguments) {
    	if (isTraceEnabled()) {
    		trace(String.format(format, arguments));
    	}
    }

    /**
     * Logs an error with trace log level.
     *
     * @param e log this cause
     */
    void trace(Throwable e);

    /**
     * Logs an error with trace log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void trace(String msg, Throwable e);
    
    /**
     * Logs an error with trace log level.
     * 
     * @param e         log this cause
     * @param format    the format string
     * @param arguments arguments
     */
    default void trace(Throwable e, String format, Object... arguments) {
    	if (isTraceEnabled()) {
    		trace(String.format(format, arguments), e);
    	}
    }

    /**
     * Logs a message with debug log level.
     *
     * @param msg log this message
     */
    void debug(String msg);
    
    /**
     * Logs a message with debug log level.
     * 
     * @param format    the format string
     * @param arguments arguments
     */
    default void debug(String format, Object... arguments) {
    	if (isTraceEnabled()) {
    		debug(String.format(format, arguments));
    	}
    }
    
    /**
     * Logs an error with debug log level.
     *
     * @param e log this cause
     */
    void debug(Throwable e);

    /**
     * Logs an error with debug log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void debug(String msg, Throwable e);
    
    /**
     * Logs an error with debug log level.
     * 
     * @param e         log this cause
     * @param format    the format string
     * @param arguments arguments
     */
    default void debug(Throwable e, String format, Object... arguments) {
    	if (isDebugEnabled()) {
    		debug(String.format(format, arguments), e);
    	}
    }
    
    /**
     * Logs a message with info log level.
     *
     * @param msg log this message
     */
    void info(String msg);
    
    /**
     * Logs a message with info log level.
     * 
     * @param format    the format string
     * @param arguments arguments
     */
    default void info(String format, Object... arguments) {
    	if (isInfoEnabled()) {
    		info(String.format(format, arguments));
    	}
    }

    /**
     * Logs an error with info log level.
     *
     * @param e log this cause
     */
    void info(Throwable e);

    /**
     * Logs an error with info log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void info(String msg, Throwable e);
    
    /**
     * Logs an error with info log level.
     * 
     * @param e         log this cause
     * @param format    the format string
     * @param arguments arguments
     */
    default void info(Throwable e, String format, Object... arguments) {
    	if (isInfoEnabled()) {
    		info(String.format(format, arguments), e);
    	}
    }
    
    /**
     * Logs a message with warn log level.
     *
     * @param msg log this message
     */
    void warn(String msg);
    
    /**
     * Logs a message with warn log level.
     * 
     * @param format    the format string
     * @param arguments arguments
     */
    default void warn(String format, Object... arguments) {
    	if (isWarnEnabled()) {
    		warn(String.format(format, arguments));
    	}
    }

    /**
     * Logs a message with warn log level.
     *
     * @param e log this message
     */
    void warn(Throwable e);

    /**
     * Logs a message with warn log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void warn(String msg, Throwable e);
    
    /**
     * Logs an error with warn log level.
     * 
     * @param e         log this cause
     * @param format    the format string
     * @param arguments arguments
     */
    default void warn(Throwable e, String format, Object... arguments) {
    	if (isWarnEnabled()) {
    		warn(String.format(format, arguments), e);
    	}
    }
    
    /**
     * Logs a message with error log level.
     *
     * @param msg log this message
     */
    void error(String msg);
    
    /**
     * Logs a message with error log level.
     * 
     * @param format    the format string
     * @param arguments arguments
     */
    default void error(String format, Object... arguments) {
    	if (isErrorEnabled()) {
    		error(String.format(format, arguments));
    	}
    }

    /**
     * Logs an error with error log level.
     *
     * @param e log this cause
     */
    void error(Throwable e);

    /**
     * Logs an error with error log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void error(String msg, Throwable e);
    
    /**
     * Logs an error with error log level.
     * 
     * @param e         log this cause
     * @param format    the format string
     * @param arguments arguments
     */
    default void error(Throwable e, String format, Object... arguments) {
    	if (isErrorEnabled()) {
    		error(String.format(format, arguments), e);
    	}
    }
    
    /**
     * Is trace logging currently enabled?
     *
     * @return true if trace is enabled
     */
    boolean isTraceEnabled();

    /**
     * Is debug logging currently enabled?
     *
     * @return true if debug is enabled
     */
    boolean isDebugEnabled();

    /**
     * Is info logging currently enabled?
     *
     * @return true if info is enabled
     */
    boolean isInfoEnabled();

    /**
     * Is warn logging currently enabled?
     *
     * @return true if warn is enabled
     */
    boolean isWarnEnabled();

    /**
     * Is error logging currently enabled?
     *
     * @return true if error is enabled
     */
    boolean isErrorEnabled();

}