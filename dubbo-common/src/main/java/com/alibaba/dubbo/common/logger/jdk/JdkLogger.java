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
package com.alibaba.dubbo.common.logger.jdk;

import com.alibaba.dubbo.common.logger.Logger;

import java.util.logging.Level;

public class JdkLogger implements Logger {

    private final java.util.logging.Logger logger;

    public JdkLogger(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public void trace(String msg) {
        logger.log(Level.FINER, msg);
    }

    public void trace(Throwable e) {
        logger.log(Level.FINER, e.getMessage(), e);
    }

    public void trace(String msg, Throwable e) {
        logger.log(Level.FINER, msg, e);
    }

    public void debug(String msg) {
        logger.log(Level.FINE, msg);
    }

    public void debug(Throwable e) {
        logger.log(Level.FINE, e.getMessage(), e);
    }

    public void debug(String msg, Throwable e) {
        logger.log(Level.FINE, msg, e);
    }

    public void info(String msg) {
        logger.log(Level.INFO, msg);
    }

    public void info(String msg, Throwable e) {
        logger.log(Level.INFO, msg, e);
    }

    public void warn(String msg) {
        logger.log(Level.WARNING, msg);
    }

    public void warn(String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
    }

    public void error(String msg) {
        logger.log(Level.SEVERE, msg);
    }

    public void error(String msg, Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }

    public void error(Throwable e) {
        logger.log(Level.SEVERE, e.getMessage(), e);
    }

    public void info(Throwable e) {
        logger.log(Level.INFO, e.getMessage(), e);
    }

    public void warn(Throwable e) {
        logger.log(Level.WARNING, e.getMessage(), e);
    }

    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

}