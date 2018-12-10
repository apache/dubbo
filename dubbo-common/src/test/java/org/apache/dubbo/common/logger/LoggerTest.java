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
import org.apache.dubbo.common.logger.slf4j.Slf4jLoggerAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


@RunWith(Parameterized.class)
public class LoggerTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {JclLoggerAdapter.class},
                {JdkLoggerAdapter.class},
                {Log4jLoggerAdapter.class},
                {Slf4jLoggerAdapter.class}
        });
    }

    private Logger logger;

    public LoggerTest(Class<? extends LoggerAdapter> loggerAdapter) throws Exception {
        LoggerAdapter adapter = loggerAdapter.newInstance();
        adapter.setLevel(Level.ALL);
        this.logger = adapter.getLogger(this.getClass());
    }

    @Test
    public void testAllLogMethod() {
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

    @Test
    public void testLevelEnable() {
        assertThat(logger.isWarnEnabled(), not(nullValue()));
        assertThat(logger.isTraceEnabled(), not(nullValue()));
        assertThat(logger.isErrorEnabled(), not(nullValue()));
        assertThat(logger.isInfoEnabled(), not(nullValue()));
        assertThat(logger.isDebugEnabled(), not(nullValue()));
    }
}