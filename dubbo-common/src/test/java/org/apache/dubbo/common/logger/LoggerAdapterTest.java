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
import org.apache.dubbo.common.logger.slf4j.Slf4jLogger;
import org.apache.dubbo.common.logger.slf4j.Slf4jLoggerAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class LoggerAdapterTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {JclLoggerAdapter.class, JclLogger.class},
                {JdkLoggerAdapter.class, JdkLogger.class},
                {Log4jLoggerAdapter.class, Log4jLogger.class},
                {Slf4jLoggerAdapter.class, Slf4jLogger.class}
        });
    }

    private Class loggerClass;
    private LoggerAdapter loggerAdapter;

    public LoggerAdapterTest(Class<? extends LoggerAdapter> loggerAdapterClass, Class<? extends Logger> loggerClass) throws Exception {
        this.loggerClass = loggerClass;
        this.loggerAdapter = loggerAdapterClass.newInstance();
    }

    @Test
    public void testGetLogger() {
        Logger logger = loggerAdapter.getLogger(this.getClass());
        assertThat(logger.getClass().isAssignableFrom(this.loggerClass), is(true));

        logger = loggerAdapter.getLogger(this.getClass().getSimpleName());
        assertThat(logger.getClass().isAssignableFrom(this.loggerClass), is(true));
    }

    @Test
    public void testLevel() {
        for (Level targetLevel : Level.values()) {
            loggerAdapter.setLevel(targetLevel);

            assertThat(loggerAdapter.getLevel(), is(targetLevel));
        }
    }
}