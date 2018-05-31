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
package com.alibaba.dubbo.common.logger;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.jcl.JclLoggerAdapter;
import com.alibaba.dubbo.common.logger.jdk.JdkLoggerAdapter;
import com.alibaba.dubbo.common.logger.log4j.Log4jLoggerAdapter;
import com.alibaba.dubbo.common.logger.slf4j.Slf4jLoggerAdapter;
import com.alibaba.dubbo.common.logger.support.FailsafeLogger;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Logger factory
 */
public class LoggerFactory {

    /**
     * 已创建的 Logger 对应的映射
     *
     * key：类名
     */
    private static final ConcurrentMap<String, FailsafeLogger> LOGGERS = new ConcurrentHashMap<String, FailsafeLogger>();
    /**
     * 当前使用的 LoggerAdapter 日志适配器
     */
    private static volatile LoggerAdapter LOGGER_ADAPTER;

    // search common-used logging frameworks
    static {
        // 获得 "logger" 配置项
        String logger = System.getProperty("dubbo.application.logger");
        // 根据配置项，进行对应的 LoggerAdapter 对象
        if ("slf4j".equals(logger)) {
            setLoggerAdapter(new Slf4jLoggerAdapter());
        } else if ("jcl".equals(logger)) {
            setLoggerAdapter(new JclLoggerAdapter());
        } else if ("log4j".equals(logger)) {
            setLoggerAdapter(new Log4jLoggerAdapter());
        } else if ("jdk".equals(logger)) {
            setLoggerAdapter(new JdkLoggerAdapter());
        } else {
            // 未配置，按照 log4j > slf4j > apache common logger > jdk logger
            try {
                setLoggerAdapter(new Log4jLoggerAdapter());
            } catch (Throwable e1) {
                try {
                    setLoggerAdapter(new Slf4jLoggerAdapter());
                } catch (Throwable e2) {
                    try {
                        setLoggerAdapter(new JclLoggerAdapter());
                    } catch (Throwable e3) {
                        setLoggerAdapter(new JdkLoggerAdapter());
                    }
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
            // 获得 Logger 对象，并打印日志，提示设置后的 LoggerAdapter 实现类
            Logger logger = loggerAdapter.getLogger(LoggerFactory.class.getName());
            logger.info("using logger: " + loggerAdapter.getClass().getName());
            // 设置 LOGGER_ADAPTER 属性
            LoggerFactory.LOGGER_ADAPTER = loggerAdapter;
            // 循环，将原有已经生成的 LOGGER 缓存对象，全部重新生成替换
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
        // 从缓存中，获得 Logger 对象
        FailsafeLogger logger = LOGGERS.get(key.getName());
        // 不存在，则进行创建，并进行缓存
        if (logger == null) {
            LOGGERS.putIfAbsent(key.getName(), new FailsafeLogger(LOGGER_ADAPTER.getLogger(key)));
            logger = LOGGERS.get(key.getName());
        }
        return logger;
    }

    /**
     * Get logger provider
     *
     * @param key the returned logger will be named after key
     * @return logger provider
     */
    public static Logger getLogger(String key) {
        // 从缓存中，获得 Logger 对象
        FailsafeLogger logger = LOGGERS.get(key);
        // 不存在，则进行创建，并进行缓存
        if (logger == null) {
            LOGGERS.putIfAbsent(key, new FailsafeLogger(LOGGER_ADAPTER.getLogger(key)));
            logger = LOGGERS.get(key);
        }
        return logger;
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