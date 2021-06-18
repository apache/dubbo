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
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Slf4jLoggerTest {
    @Test
    public void testLocationAwareLogger() {
        LocationAwareLogger locationAwareLogger = mock(LocationAwareLogger.class);
        Slf4jLogger logger = new Slf4jLogger(locationAwareLogger);

        logger.error("error");

        logger.warn("warn");
        logger.info("info");
        logger.debug("debug");
        logger.trace("info");

        verify(locationAwareLogger, times(5)).log(isNull(Marker.class), anyString(),
                anyInt(), anyString(), isNull(Object[].class), isNull(Throwable.class));

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

        verify(locationAwareLogger, times(10)).log(isNull(Marker.class), anyString(),
                anyInt(), anyString(), isNull(Object[].class), any(Throwable.class));
    }
}
