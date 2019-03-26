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
package org.apache.dubbo.common.logger.support;

import org.apache.dubbo.common.logger.Logger;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FailsafeLoggerTest {
    @Test
    public void testFailSafeForLoggingMethod() {
        Logger failLogger = mock(Logger.class);
        FailsafeLogger failsafeLogger = new FailsafeLogger(failLogger);

        doThrow(new RuntimeException()).when(failLogger).error(anyString());
        doThrow(new RuntimeException()).when(failLogger).warn(anyString());
        doThrow(new RuntimeException()).when(failLogger).info(anyString());
        doThrow(new RuntimeException()).when(failLogger).debug(anyString());
        doThrow(new RuntimeException()).when(failLogger).trace(anyString());

        failsafeLogger.error("error");
        failsafeLogger.warn("warn");
        failsafeLogger.info("info");
        failsafeLogger.debug("debug");
        failsafeLogger.trace("info");

        doThrow(new RuntimeException()).when(failLogger).error(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).warn(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).info(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).debug(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).trace(any(Throwable.class));

        failsafeLogger.error(new Exception("error"));
        failsafeLogger.warn(new Exception("warn"));
        failsafeLogger.info(new Exception("info"));
        failsafeLogger.debug(new Exception("debug"));
        failsafeLogger.trace(new Exception("trace"));

        failsafeLogger.error("error", new Exception("error"));
        failsafeLogger.warn("warn", new Exception("warn"));
        failsafeLogger.info("info", new Exception("info"));
        failsafeLogger.debug("debug", new Exception("debug"));
        failsafeLogger.trace("trace", new Exception("trace"));
    }

    @Test
    public void testSuccessLogger() {
        Logger successLogger = mock(Logger.class);
        FailsafeLogger failsafeLogger = new FailsafeLogger(successLogger);
        failsafeLogger.error("error");
        failsafeLogger.warn("warn");
        failsafeLogger.info("info");
        failsafeLogger.debug("debug");
        failsafeLogger.trace("info");

        verify(successLogger).error(anyString());
        verify(successLogger).warn(anyString());
        verify(successLogger).info(anyString());
        verify(successLogger).debug(anyString());
        verify(successLogger).trace(anyString());

        failsafeLogger.error(new Exception("error"));
        failsafeLogger.warn(new Exception("warn"));
        failsafeLogger.info(new Exception("info"));
        failsafeLogger.debug(new Exception("debug"));
        failsafeLogger.trace(new Exception("trace"));

        failsafeLogger.error("error", new Exception("error"));
        failsafeLogger.warn("warn", new Exception("warn"));
        failsafeLogger.info("info", new Exception("info"));
        failsafeLogger.debug("debug", new Exception("debug"));
        failsafeLogger.trace("trace", new Exception("trace"));
    }

    @Test(expected = RuntimeException.class)
    public void testGetLogger() {
        Logger failLogger = mock(Logger.class);
        FailsafeLogger failsafeLogger = new FailsafeLogger(failLogger);

        doThrow(new RuntimeException()).when(failLogger).error(anyString());
        failsafeLogger.getLogger().error("should get error");
    }
}