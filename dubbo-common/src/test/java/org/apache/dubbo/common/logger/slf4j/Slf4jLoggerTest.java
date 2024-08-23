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
package org.apache.dubbo.common.logger.slf4j;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.spi.LocationAwareLogger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class Slf4jLoggerTest {
    @Test
    void testLocationAwareLogger() {
        LocationAwareLogger locationAwareLogger = mock(LocationAwareLogger.class);

        Slf4jLogger logger = new Slf4jLogger(locationAwareLogger);

        logger.error("error");
        logger.warn("warn");
        logger.info("info");
        logger.debug("debug");
        logger.trace("info");

        verify(locationAwareLogger, times(5)).log(isNull(), anyString(), anyInt(), anyString(), isNull(), isNull());

        logger.error("error:{}", "arg1");
        logger.warn("warn:{}", "arg1");
        logger.info("info:{}", "arg1");
        logger.debug("debug:{}", "arg1");
        logger.trace("info:{}", "arg1");

        verify(locationAwareLogger, never())
                .log(isNull(), anyString(), anyInt(), anyString(), eq(new String[] {"arg1"}), isNull());

        Mockito.when(locationAwareLogger.isErrorEnabled()).thenReturn(true);
        Mockito.when(locationAwareLogger.isWarnEnabled()).thenReturn(true);
        Mockito.when(locationAwareLogger.isInfoEnabled()).thenReturn(true);
        Mockito.when(locationAwareLogger.isDebugEnabled()).thenReturn(true);
        Mockito.when(locationAwareLogger.isTraceEnabled()).thenReturn(true);

        logger.error("error:{}", "arg1");
        logger.warn("warn:{}", "arg1");
        logger.info("info:{}", "arg1");
        logger.debug("debug:{}", "arg1");
        logger.trace("info:{}", "arg1");

        verify(locationAwareLogger, times(5))
                .log(isNull(), anyString(), anyInt(), anyString(), eq(new String[] {"arg1"}), isNull());

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

        verify(locationAwareLogger, times(10))
                .log(isNull(), anyString(), anyInt(), anyString(), isNull(), any(Throwable.class));

        logger.error("error:{}", "arg1", new Exception("error"));
        logger.warn("warn:{}", "arg1", new Exception("warn"));
        logger.info("info:{}", "arg1", new Exception("info"));
        logger.debug("debug:{}", "arg1", new Exception("debug"));
        logger.trace("trace:{}", "arg1", new Exception("trace"));

        verify(locationAwareLogger, times(5))
                .log(isNull(), anyString(), anyInt(), anyString(), eq(new String[] {"arg1"}), any(Throwable.class));
    }
}
