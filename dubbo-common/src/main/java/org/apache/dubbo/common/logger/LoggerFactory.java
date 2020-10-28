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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.jcl.JclLoggerAdapter;
import org.apache.dubbo.common.logger.jdk.JdkLoggerAdapter;
import org.apache.dubbo.common.logger.log4j.Log4jLoggerAdapter;
import org.apache.dubbo.common.logger.log4j2.Log4j2LoggerAdapter;
import org.apache.dubbo.common.logger.slf4j.Slf4jLoggerAdapter;
import org.apache.dubbo.common.logger.support.FailsafeLogger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Logger factory
 */
public class LoggerFactory {

    private static final ConcurrentMap<String, FailsafeLogger> LOGGERS = new ConcurrentHashMap<>();
    private static volatile LoggerAdapter LOGGER_ADAPTER;

    // search common-used logging frameworks
    static {
        String logger = System.getProperty("dubbo.application.logger", "");
        switch (logger) {
            case "slf4j":
                setLoggerAdapter(new Slf4jLoggerAdapter());
                break;
            case "jcl":
                setLoggerAdapter(new JclLoggerAdapter());
                break;
            case "log4j":
                setLoggerAdapter(new Log4jLoggerAdapter());
                break;
            case "jdk":
                setLoggerAdapter(new JdkLoggerAdapter());
                break;
            case "log4j2":
                setLoggerAdapter(new Log4j2LoggerAdapter());
                break;
            default:
                List<Class<? extends LoggerAdapter>> candidates = Arrays.asList(
                        Log4jLoggerAdapter.class,
                        Slf4jLoggerAdapter.class,
                        Log4j2LoggerAdapter.class,
                        JclLoggerAdapter.class,
                        JdkLoggerAdapter.class
                );
                for (Class<? extends LoggerAdapter> clazz : candidates) {
                    try {
                        setLoggerAdapter(clazz.newInstance());
                        break;
                    } catch (Throwable ignored) {
                    }
                }
        }
    }

    private LoggerFactory() {
    }

    public static void setLoggerAdapter(String loggerAdapter) {
        if (loggerAdapter != null && loggerAdapter.length() > 0) {
            setLoggerAdapter(ExtensionLoader.getExtensionLoader(LoggerAdapter.class).getExtension(loggerAdapter));
        }
    }

    /**
     * Set logger provider
     *
     * @param loggerAdapter logger provider
     */
    public static void setLoggerAdapter(LoggerAdapter loggerAdapter) {
        if (loggerAdapter != null) {
            Logger logger = loggerAdapter.getLogger(LoggerFactory.class.getName());
            logger.info("using logger: " + loggerAdapter.getClass().getName());
            LoggerFactory.LOGGER_ADAPTER = loggerAdapter;
            for (Map.Entry<String, FailsafeLogger> entry : LOGGERS.entrySet()) {
                entry.getValue().setLogger(LOGGER_ADAPTER.getLogger(entry.getKey()));
            }
        }
    }

    /**
     * Get logger provider
     *
     * @param key the returned logger will be named after clazz
     * @return logger
     */
    public static Logger getLogger(Class<?> key) {
        return LOGGERS.computeIfAbsent(key.getName(), name -> new FailsafeLogger(LOGGER_ADAPTER.getLogger(name)));
    }

    /**
     * Get logger provider
     *
     * @param key the returned logger will be named after key
     * @return logger provider
     */
    public static Logger getLogger(String key) {
        return LOGGERS.computeIfAbsent(key, k -> new FailsafeLogger(LOGGER_ADAPTER.getLogger(k)));
    }

    /**
     * Get logging level
     *
     * @return logging level
     */
    public static Level getLevel() {
        return LOGGER_ADAPTER.getLevel();
    }

    /**
     * Set the current logging level
     *
     * @param level logging level
     */
    public static void setLevel(Level level) {
        LOGGER_ADAPTER.setLevel(level);
    }

    /**
     * Get the current logging file
     *
     * @return current logging file
     */
    public static File getFile() {
        return LOGGER_ADAPTER.getFile();
    }

}
