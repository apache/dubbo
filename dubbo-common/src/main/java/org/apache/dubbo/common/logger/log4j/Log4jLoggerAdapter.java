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
package org.apache.dubbo.common.logger.log4j;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerAdapter;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Log4jLoggerAdapter implements LoggerAdapter {

    private File file;

    private static final Map<Level, org.apache.log4j.Level> toLevel = new HashMap<>();
    private static final Map<org.apache.log4j.Level, Level> toApacheLevel = new HashMap<>();

    static {
        toLevel.put(Level.ALL, org.apache.log4j.Level.ALL);
        toLevel.put(Level.TRACE, org.apache.log4j.Level.TRACE);
        toLevel.put(Level.DEBUG, org.apache.log4j.Level.DEBUG);
        toLevel.put(Level.INFO, org.apache.log4j.Level.INFO);
        toLevel.put(Level.WARN, org.apache.log4j.Level.WARN);
        toLevel.put(Level.ERROR, org.apache.log4j.Level.ERROR);

        toApacheLevel.put(org.apache.log4j.Level.ALL, Level.ALL);
        toApacheLevel.put(org.apache.log4j.Level.TRACE, Level.TRACE);
        toApacheLevel.put(org.apache.log4j.Level.DEBUG, Level.DEBUG);
        toApacheLevel.put(org.apache.log4j.Level.INFO, Level.INFO);
        toApacheLevel.put(org.apache.log4j.Level.WARN, Level.WARN);
        toApacheLevel.put(org.apache.log4j.Level.ERROR, Level.ERROR);
    }

    @SuppressWarnings("unchecked")
    public Log4jLoggerAdapter() {
        try {
            org.apache.log4j.Logger logger = LogManager.getRootLogger();
            if (logger != null) {
                Enumeration<Appender> appenders = logger.getAllAppenders();
                if (appenders != null) {
                    while (appenders.hasMoreElements()) {
                        Appender appender = appenders.nextElement();
                        if (appender instanceof FileAppender) {
                            FileAppender fileAppender = (FileAppender) appender;
                            String filename = fileAppender.getFile();
                            file = new File(filename);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
        }
    }

    private static org.apache.log4j.Level toLog4jLevel(Level level) {
        return toLevel.getOrDefault(level, org.apache.log4j.Level.OFF);
    }

    private static Level fromLog4jLevel(org.apache.log4j.Level level) {
        return toApacheLevel.getOrDefault(level, Level.OFF);
    }

    @Override
    public Logger getLogger(Class<?> key) {
        return new Log4jLogger(LogManager.getLogger(key));
    }

    @Override
    public Logger getLogger(String key) {
        return new Log4jLogger(LogManager.getLogger(key));
    }

    @Override
    public Level getLevel() {
        return fromLog4jLevel(LogManager.getRootLogger().getLevel());
    }

    @Override
    public void setLevel(Level level) {
        LogManager.getRootLogger().setLevel(toLog4jLevel(level));
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {

    }

}
