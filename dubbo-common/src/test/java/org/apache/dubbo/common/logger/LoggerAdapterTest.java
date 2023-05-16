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

import org.apache.dubbo.common.logger.jcl.JclLogger;
import org.apache.dubbo.common.logger.jcl.JclLoggerAdapter;
import org.apache.dubbo.common.logger.jdk.JdkLogger;
import org.apache.dubbo.common.logger.jdk.JdkLoggerAdapter;
import org.apache.dubbo.common.logger.log4j.Log4jLogger;
import org.apache.dubbo.common.logger.log4j.Log4jLoggerAdapter;
import org.apache.dubbo.common.logger.log4j2.Log4j2Logger;
import org.apache.dubbo.common.logger.log4j2.Log4j2LoggerAdapter;
import org.apache.dubbo.common.logger.slf4j.Slf4jLogger;
import org.apache.dubbo.common.logger.slf4j.Slf4jLoggerAdapter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class LoggerAdapterTest {
    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(JclLoggerAdapter.class, JclLogger.class),
                Arguments.of(JdkLoggerAdapter.class, JdkLogger.class),
                Arguments.of(Log4jLoggerAdapter.class, Log4jLogger.class),
                Arguments.of(Slf4jLoggerAdapter.class, Slf4jLogger.class),
                Arguments.of(Log4j2LoggerAdapter.class, Log4j2Logger.class)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void testGetLogger(Class<? extends LoggerAdapter> loggerAdapterClass, Class<? extends Logger> loggerClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LoggerAdapter loggerAdapter = loggerAdapterClass.getDeclaredConstructor().newInstance();
        Logger logger = loggerAdapter.getLogger(this.getClass());
        assertThat(logger.getClass().isAssignableFrom(loggerClass), is(true));

        logger = loggerAdapter.getLogger(this.getClass().getSimpleName());
        assertThat(logger.getClass().isAssignableFrom(loggerClass), is(true));

    }

    @ParameterizedTest
    @MethodSource("data")
    void testLevel(Class<? extends LoggerAdapter> loggerAdapterClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LoggerAdapter loggerAdapter = loggerAdapterClass.getDeclaredConstructor().newInstance();
        for (Level targetLevel : Level.values()) {
            loggerAdapter.setLevel(targetLevel);
            assertThat(loggerAdapter.getLevel(), is(targetLevel));
        }
    }
}
