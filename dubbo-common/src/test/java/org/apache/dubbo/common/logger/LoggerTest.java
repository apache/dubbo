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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class LoggerTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(JclLoggerAdapter.class),
                Arguments.of(JdkLoggerAdapter.class),
                Arguments.of(Log4jLoggerAdapter.class),
                Arguments.of(Slf4jLoggerAdapter.class),
                Arguments.of(Log4j2LoggerAdapter.class)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void testAllLogMethod(Class<? extends LoggerAdapter> loggerAdapter) throws Exception {
        LoggerAdapter adapter = loggerAdapter.getDeclaredConstructor().newInstance();
        adapter.setLevel(Level.ALL);
        Logger logger = adapter.getLogger(this.getClass());
        logger.error("error");
        logger.warn("warn");
        logger.info("info");
        logger.debug("debug");
        logger.trace("info");

        logger.error(new Exception("error"));
        logger.warn(new Exception("warn"));
        logger.info(new Exception("info"));
        logger.debug(new Exception("debug"));
        logger.trace(new Exception("trace"));

        logger.error("error", new Exception("error"));
        logger.warn("warn", new Exception("warn"));
        logger.info("info", new Exception("info"));
        logger.debug("debug", new Exception("debug"));
        logger.trace("trace", new Exception("trace"));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testLevelEnable(Class<? extends LoggerAdapter> loggerAdapter) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LoggerAdapter adapter = loggerAdapter.getDeclaredConstructor().newInstance();
        adapter.setLevel(Level.ALL);
        Logger logger = adapter.getLogger(this.getClass());
        assertThat(logger.isWarnEnabled(), not(nullValue()));
        assertThat(logger.isTraceEnabled(), not(nullValue()));
        assertThat(logger.isErrorEnabled(), not(nullValue()));
        assertThat(logger.isInfoEnabled(), not(nullValue()));
        assertThat(logger.isDebugEnabled(), not(nullValue()));
    }
}
