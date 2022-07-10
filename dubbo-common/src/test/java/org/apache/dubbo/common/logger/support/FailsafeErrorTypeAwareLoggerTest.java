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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for FailsafeErrorTypeAwareLogger to test whether it 'ignores' exceptions thrown by logger or not.
 */
public class FailsafeErrorTypeAwareLoggerTest {
    @Test
    public void testFailsafeErrorTypeAwareForLoggingMethod() {
        Logger failLogger = mock(Logger.class);
        FailsafeErrorTypeAwareLogger failsafeLogger = new FailsafeErrorTypeAwareLogger(failLogger);

        doThrow(new RuntimeException()).when(failLogger).error(anyString());
        doThrow(new RuntimeException()).when(failLogger).warn(anyString());
        doThrow(new RuntimeException()).when(failLogger).info(anyString());
        doThrow(new RuntimeException()).when(failLogger).debug(anyString());
        doThrow(new RuntimeException()).when(failLogger).trace(anyString());

        failsafeLogger.error("1-1", "Registry center", "May be it's offline.", "error");
        failsafeLogger.warn("1-1", "Registry center", "May be it's offline.", "warn");
        failsafeLogger.info("1-1", "Registry center", "May be it's offline.", "info");
        failsafeLogger.debug("1-1", "Registry center", "May be it's offline.", "debug");
        failsafeLogger.trace("1-1", "Registry center", "May be it's offline.", "info");

        doThrow(new RuntimeException()).when(failLogger).error(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).warn(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).info(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).debug(any(Throwable.class));
        doThrow(new RuntimeException()).when(failLogger).trace(any(Throwable.class));

        failsafeLogger.error("1-1", "Registry center", "May be it's offline.", "error", new Exception("error"));
        failsafeLogger.warn("1-1", "Registry center", "May be it's offline.", "warn", new Exception("warn"));
        failsafeLogger.info("1-1", "Registry center", "May be it's offline.", "info", new Exception("info"));
        failsafeLogger.debug("1-1", "Registry center", "May be it's offline.", "debug", new Exception("debug"));
        failsafeLogger.trace("1-1", "Registry center", "May be it's offline.", "trace", new Exception("trace"));
    }

    @Test
    public void testSuccessLogger() {
        Logger successLogger = mock(Logger.class);
        FailsafeErrorTypeAwareLogger failsafeLogger = new FailsafeErrorTypeAwareLogger(successLogger);

        failsafeLogger.error("1-1", "Registry center", "May be it's offline.", "error");
        failsafeLogger.warn("1-1", "Registry center", "May be it's offline.", "warn");
        failsafeLogger.info("1-1", "Registry center", "May be it's offline.", "info");
        failsafeLogger.debug("1-1", "Registry center", "May be it's offline.", "debug");
        failsafeLogger.trace("1-1", "Registry center", "May be it's offline.", "info");

        verify(successLogger).error(anyString());
        verify(successLogger).warn(anyString());
        verify(successLogger).info(anyString());
        verify(successLogger).debug(anyString());
        verify(successLogger).trace(anyString());

        failsafeLogger.error("1-1", "Registry center", "May be it's offline.", "error", new Exception("error"));
        failsafeLogger.warn("1-1", "Registry center", "May be it's offline.", "warn", new Exception("warn"));
        failsafeLogger.info("1-1", "Registry center", "May be it's offline.", "info", new Exception("info"));
        failsafeLogger.debug("1-1", "Registry center", "May be it's offline.", "debug", new Exception("debug"));
        failsafeLogger.trace("1-1", "Registry center", "May be it's offline.", "trace", new Exception("trace"));
    }

    @Test
    public void testGetLogger() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Logger failLogger = mock(Logger.class);
            FailsafeErrorTypeAwareLogger failsafeLogger = new FailsafeErrorTypeAwareLogger(failLogger);

            doThrow(new RuntimeException()).when(failLogger).error(anyString());
            failsafeLogger.getLogger().error("should get error");
        });
    }

    @Test
    public void testInstructionShownOrNot() {
        LoggerFactory.setLoggerAdapter(FrameworkModel.defaultModel(), "jdk");

        ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(FailsafeErrorTypeAwareLoggerTest.class);

        logger.error("1-1", "Registry center", "May be it's offline.",
            "error message", new Exception("error"));
    }
}
