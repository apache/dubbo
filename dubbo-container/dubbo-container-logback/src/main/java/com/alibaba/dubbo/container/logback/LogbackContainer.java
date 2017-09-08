/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.container.logback;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.container.Container;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;

/**
 * LogbackContainer. (SPI, Singleton, ThreadSafe)
 *
 * @author {@link "mailto:qq55355383@gmail.com" "yi.tong"}
 * @date May 30, 2012
 */
public class LogbackContainer implements Container {

    public static final String LOGBACK_FILE = "dubbo.logback.file";

    public static final String LOGBACK_LEVEL = "dubbo.logback.level";

    public static final String LOGBACK_MAX_HISTORY = "dubbo.logback.maxhistory";

    public static final String DEFAULT_LOGBACK_LEVEL = "ERROR";

    public void start() {
        String file = ConfigUtils.getProperty(LOGBACK_FILE);
        if (file != null && file.length() > 0) {
            String level = ConfigUtils.getProperty(LOGBACK_LEVEL);
            if (level == null || level.length() == 0) {
                level = DEFAULT_LOGBACK_LEVEL;
            }
            // maxHistory=0 Infinite history
            int maxHistory = StringUtils.parseInteger(ConfigUtils.getProperty(LOGBACK_MAX_HISTORY));

            doInitializer(file, level, maxHistory);
        }
    }

    public void stop() {
    }

    /**
     * Initializer logback
     *
     * @param file
     * @param level
     * @param maxHistory
     */
    private void doInitializer(String file, String level, int maxHistory) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();

        // appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("application");
        fileAppender.setFile(file);
        fileAppender.setAppend(true);

        // policy
        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<ILoggingEvent>();
        policy.setContext(loggerContext);
        policy.setMaxHistory(maxHistory);
        policy.setFileNamePattern(file + ".%d{yyyy-MM-dd}");
        policy.setParent(fileAppender);
        policy.start();
        fileAppender.setRollingPolicy(policy);

        // encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%date [%thread] %-5level %logger (%file:%line\\) - %msg%n");
        encoder.start();
        fileAppender.setEncoder(encoder);

        fileAppender.start();

        rootLogger.addAppender(fileAppender);
        rootLogger.setLevel(Level.toLevel(level));
        rootLogger.setAdditive(false);
    }

}