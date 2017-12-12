/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.logger.log4j2;

import com.alibaba.dubbo.common.logger.Level;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.File;
import java.util.Map;

public class Log4j2LoggerAdapter implements LoggerAdapter {

    private File file;

    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    @SuppressWarnings("unchecked")
    public Log4j2LoggerAdapter() {

        try {
            LoggerContext lcx = (LoggerContext) LogManager.getContext(false);

            Map<String, Appender> appenders = lcx.getConfiguration().getAppenders();
            if (appenders != null) {
                for (String key : appenders.keySet()) {
                    Appender appender = appenders.get(key);
                    if (appender instanceof FileAppender) {
                        FileAppender fileAppender = (FileAppender) appender;
                        String filename = fileAppender.getFileName();
                        this.file = new File(filename);
                        break;
                    } else if (appender instanceof RollingFileAppender) {
                        RollingFileAppender fileAppender = (RollingFileAppender) appender;
                        String filename = fileAppender.getFileName();
                        this.file = new File(filename);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private String getDefaultLoggingRoot() {
        return new File(System.getProperty("user.home") + "/logs").getAbsolutePath();
    }

    public Logger getLogger(Class<?> key) {
        return new Log4j2Logger(key);
    }

    public Logger getLogger(String key) {
        return new Log4j2Logger(key);
    }

    public void setLevel(Level level) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        config.getRootLogger().setLevel(toLog4jLevel(level));
        ctx.updateLoggers();
    }

    public Level getLevel() {
        return fromLog4jLevel(LogManager.getRootLogger().getLevel());
    }

    public File getFile() {
        return file;
    }

    private static org.apache.logging.log4j.Level toLog4jLevel(Level level) {
        if (level == Level.ALL)
            return org.apache.logging.log4j.Level.ALL;
        if (level == Level.TRACE)
            return org.apache.logging.log4j.Level.TRACE;
        if (level == Level.DEBUG)
            return org.apache.logging.log4j.Level.DEBUG;
        if (level == Level.INFO)
            return org.apache.logging.log4j.Level.INFO;
        if (level == Level.WARN)
            return org.apache.logging.log4j.Level.WARN;
        if (level == Level.ERROR)
            return org.apache.logging.log4j.Level.ERROR;
        // if (level == Level.OFF)
        return org.apache.logging.log4j.Level.OFF;
    }

    private static Level fromLog4jLevel(org.apache.logging.log4j.Level level) {
        if (level == org.apache.logging.log4j.Level.ALL)
            return Level.ALL;
        if (level == org.apache.logging.log4j.Level.TRACE)
            return Level.TRACE;
        if (level == org.apache.logging.log4j.Level.DEBUG)
            return Level.DEBUG;
        if (level == org.apache.logging.log4j.Level.INFO)
            return Level.INFO;
        if (level == org.apache.logging.log4j.Level.WARN)
            return Level.WARN;
        if (level == org.apache.logging.log4j.Level.ERROR)
            return Level.ERROR;
        // if (level == org.apache.log4j.Level.OFF)
        return Level.OFF;
    }

    public void setFile(File file) {

    }

}


//            System.setProperty("logFileName", getDefaultLoggingRoot());
//
//                    if (lcx != null) {
//                    Map<String, Appender> appenders = lcx.getConfiguration().getAppenders();
//        if (appenders != null) {
//        for (String key : appenders.keySet()) {
//        Appender appender = appenders.get(key);
//        if (appender instanceof FileAppender) {
//        FileAppender fileAppender = (FileAppender) appender;
//        String filename = fileAppender.getFileName();
//        file = new File(filename);
//        break;
//        } else if (appender instanceof RollingFileAppender) {
//        RollingFileAppender fileAppender = (RollingFileAppender) appender;
//        String filename = fileAppender.getFileName();
//        file = new File(filename);
//        break;
//        }
//        }
//        }
//        }