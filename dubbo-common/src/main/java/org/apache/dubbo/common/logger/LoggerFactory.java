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

import org.apache.dubbo.common.logger.jcl.JclLoggerAdapter;
import org.apache.dubbo.common.logger.jdk.JdkLoggerAdapter;
import org.apache.dubbo.common.logger.log4j.Log4jLoggerAdapter;
import org.apache.dubbo.common.logger.log4j2.Log4j2LoggerAdapter;
import org.apache.dubbo.common.logger.slf4j.Slf4jLoggerAdapter;
import org.apache.dubbo.common.logger.support.FailsafeErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Logger factory
 */
public class LoggerFactory {

    private static final ConcurrentMap<String, FailsafeLogger> LOGGERS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, FailsafeErrorTypeAwareLogger> ERROR_TYPE_AWARE_LOGGERS = new ConcurrentHashMap<>();
    private static volatile LoggerAdapter loggerAdapter;

    // search common-used logging frameworks
    static {
        String logger = System.getProperty("dubbo.application.logger", "");
        switch (logger) {
            case Slf4jLoggerAdapter.NAME:
                setLoggerAdapter(new Slf4jLoggerAdapter());
                break;
            case JclLoggerAdapter.NAME:
                setLoggerAdapter(new JclLoggerAdapter());
                break;
            case Log4jLoggerAdapter.NAME:
                setLoggerAdapter(new Log4jLoggerAdapter());
                break;
            case JdkLoggerAdapter.NAME:
                setLoggerAdapter(new JdkLoggerAdapter());
                break;
            case Log4j2LoggerAdapter.NAME:
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
                boolean found = false;
                // try to use the first available adapter
                for (Class<? extends LoggerAdapter> clazz : candidates) {
                    try {
                        LoggerAdapter loggerAdapter = clazz.getDeclaredConstructor().newInstance();
                        loggerAdapter.getLogger(LoggerFactory.class);
                        if (loggerAdapter.isConfigured()) {
                            setLoggerAdapter(loggerAdapter);
                            found = true;
                            break;
                        }
                    } catch (Exception | LinkageError ignored) {
                        // ignore
                    }
                }
                if (found) {
                    break;
                }

                System.err.println("Dubbo: Unable to find a proper configured logger to log out.");
                for (Class<? extends LoggerAdapter> clazz : candidates) {
                    try {
                        LoggerAdapter loggerAdapter = clazz.getDeclaredConstructor().newInstance();
                        loggerAdapter.getLogger(LoggerFactory.class);
                        setLoggerAdapter(loggerAdapter);
                        found = true;
                        break;
                    } catch (Throwable ignored) {
                        // ignore
                    }
                }
                if (found) {
                    System.err.println("Dubbo: Using default logger: " + loggerAdapter.getClass().getName() + ". " +
                        "If you cannot see any log, please configure -Ddubbo.application.logger property to your preferred logging framework.");
                } else {
                    System.err.println("Dubbo: Unable to find any available logger adapter to log out. Dubbo logs will be ignored. " +
                        "Please configure -Ddubbo.application.logger property and add corresponding logging library to classpath.");
                }
        }
    }

    private LoggerFactory() {
    }

    public static void setLoggerAdapter(FrameworkModel frameworkModel, String loggerAdapter) {
        if (loggerAdapter != null && loggerAdapter.length() > 0) {
            setLoggerAdapter(frameworkModel.getExtensionLoader(LoggerAdapter.class).getExtension(loggerAdapter));
        }
    }

    /**
     * Set logger provider
     *
     * @param loggerAdapter logger provider
     */
    public static void setLoggerAdapter(LoggerAdapter loggerAdapter) {
        if (loggerAdapter != null) {
            if (loggerAdapter == LoggerFactory.loggerAdapter) {
                return;
            }
            loggerAdapter.getLogger(LoggerFactory.class.getName());
            LoggerFactory.loggerAdapter = loggerAdapter;
            for (Map.Entry<String, FailsafeLogger> entry : LOGGERS.entrySet()) {
                entry.getValue().setLogger(LoggerFactory.loggerAdapter.getLogger(entry.getKey()));
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
        return ConcurrentHashMapUtils.computeIfAbsent(LOGGERS, key.getName(), name -> new FailsafeLogger(loggerAdapter.getLogger(name)));
    }

    /**
     * Get logger provider
     *
     * @param key the returned logger will be named after key
     * @return logger provider
     */
    public static Logger getLogger(String key) {
        return ConcurrentHashMapUtils.computeIfAbsent(LOGGERS, key, k -> new FailsafeLogger(loggerAdapter.getLogger(k)));
    }

    /**
     * Get error type aware logger by Class object.
     *
     * @param key the returned logger will be named after clazz
     * @return error type aware logger
     */
    public static ErrorTypeAwareLogger getErrorTypeAwareLogger(Class<?> key) {
        return ConcurrentHashMapUtils.computeIfAbsent(ERROR_TYPE_AWARE_LOGGERS, key.getName(), name -> new FailsafeErrorTypeAwareLogger(loggerAdapter.getLogger(name)));
    }

    /**
     * Get error type aware logger by a String key.
     *
     * @param key the returned logger will be named after key
     * @return error type aware logger
     */
    public static ErrorTypeAwareLogger getErrorTypeAwareLogger(String key) {
        return ConcurrentHashMapUtils.computeIfAbsent(ERROR_TYPE_AWARE_LOGGERS, key, k -> new FailsafeErrorTypeAwareLogger(loggerAdapter.getLogger(k)));
    }

    /**
     * Get logging level
     *
     * @return logging level
     */
    public static Level getLevel() {
        return loggerAdapter.getLevel();
    }

    /**
     * Set the current logging level
     *
     * @param level logging level
     */
    public static void setLevel(Level level) {
        loggerAdapter.setLevel(level);
    }

    /**
     * Get the current logging file
     *
     * @return current logging file
     */
    public static File getFile() {
        return loggerAdapter.getFile();
    }

    /**
     * Get the available adapter names
     *
     * @return available adapter names
     */
    public static List<String> getAvailableAdapter() {
        Map<Class<? extends LoggerAdapter>, String> candidates = new HashMap<>();
        candidates.put(Log4jLoggerAdapter.class, "log4j");
        candidates.put(Slf4jLoggerAdapter.class, "slf4j");
        candidates.put(Log4j2LoggerAdapter.class, "log4j2");
        candidates.put(JclLoggerAdapter.class, "jcl");
        candidates.put(JdkLoggerAdapter.class, "jdk");
        List<String> result = new LinkedList<>();
        for (Map.Entry<Class<? extends LoggerAdapter>, String> entry : candidates.entrySet()) {
            try {
                LoggerAdapter loggerAdapter = entry.getKey().getDeclaredConstructor().newInstance();
                loggerAdapter.getLogger(LoggerFactory.class);
                result.add(entry.getValue());
            } catch (Exception ignored) {
                // ignored
            }
        }
        return result;
    }

    /**
     * Get the current adapter name
     *
     * @return current adapter name
     */
    public static String getCurrentAdapter() {
        Map<Class<? extends LoggerAdapter>, String> candidates = new HashMap<>();
        candidates.put(Log4jLoggerAdapter.class, "log4j");
        candidates.put(Slf4jLoggerAdapter.class, "slf4j");
        candidates.put(Log4j2LoggerAdapter.class, "log4j2");
        candidates.put(JclLoggerAdapter.class, "jcl");
        candidates.put(JdkLoggerAdapter.class, "jdk");

        String name = candidates.get(loggerAdapter.getClass());
        if (name == null) {
            name = loggerAdapter.getClass().getSimpleName();
        }
        return name;
    }

}
