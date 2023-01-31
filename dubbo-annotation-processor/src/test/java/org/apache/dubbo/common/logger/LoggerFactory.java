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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Logger factory
 */
public class LoggerFactory {

    private static final ConcurrentMap<String, ErrorTypeAwareLogger> ERROR_TYPE_AWARE_LOGGERS = new ConcurrentHashMap<>();

    private LoggerFactory() {
    }

    /**
     * Get error type aware logger by Class object.
     *
     * @param key the returned logger will be named after clazz
     * @return error type aware logger
     */
    public static ErrorTypeAwareLogger getErrorTypeAwareLogger(Class<?> key) {
        return ERROR_TYPE_AWARE_LOGGERS.computeIfAbsent(key.getName(), name -> new ErrorTypeAwareLogger() {
            @Override
            public void warn(String code, String cause, String extendedInformation, String msg) {
            }

            @Override
            public void warn(String msg) {
            }
        });
    }
}
