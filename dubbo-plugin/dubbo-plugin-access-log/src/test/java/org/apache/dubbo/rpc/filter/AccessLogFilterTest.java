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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.logger.log4j.Log4jLogger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.support.AccessLogData;
import org.apache.dubbo.rpc.support.MockInvocation;
import org.apache.dubbo.rpc.support.MyInvoker;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * AccessLogFilterTest.java
 */
class AccessLogFilterTest {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger("mock.dubbo.access.log");
    private static org.apache.log4j.Logger realLogger;

    @BeforeAll
    public static void setupLogger() {
        FailsafeLogger failsafeLogger = (FailsafeLogger) logger;
        Log4jLogger log4j2Logger = (Log4jLogger) failsafeLogger.getLogger();
        realLogger = log4j2Logger.getLogger();
    }

    AccessLogFilter accessLogFilter = new AccessLogFilter();

    // TODO how to assert thread action
    @Test
    @SuppressWarnings("unchecked")
    public void testDefault() throws NoSuchFieldException, IllegalAccessException {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");
        Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(url);
        Invocation invocation = new MockInvocation();

        Field field = AccessLogFilter.class.getDeclaredField("logEntries");
        field.setAccessible(true);
        assertTrue(((Map) field.get(accessLogFilter)).isEmpty());

        accessLogFilter.invoke(invoker, invocation);

        Map<String, Queue<AccessLogData>> logs = (Map<String, Queue<AccessLogData>>) field.get(accessLogFilter);
        assertFalse(logs.isEmpty());
        assertFalse(logs.get("true").isEmpty());
        AccessLogData log = logs.get("true").iterator().next();
        assertEquals("org.apache.dubbo.rpc.support.DemoService", log.getServiceName());
    }

    @Test
    void testCustom() {
        ErrorTypeAwareLogger originalLogger = AccessLogFilter.logger;
        long originalInterval = AccessLogFilter.getInterval();

        Appender appender = mock(Appender.class);
        AccessLogFilter.setInterval(500);
        AccessLogFilter.logger = logger;
        AccessLogFilter customAccessLogFilter = new AccessLogFilter();
        try {
            realLogger.addAppender(appender);
            URL url = URL.valueOf("test://test:11/test?accesslog=custom-access.log");
            Invoker<AccessLogFilterTest> invoker = new MyInvoker<>(url);
            Invocation invocation = new MockInvocation();
            customAccessLogFilter.invoke(invoker, invocation);
            sleep();
            ArgumentCaptor<LoggingEvent> argument = ArgumentCaptor.forClass(LoggingEvent.class);
            verify(appender, times(2)).doAppend(argument.capture());
            assertEquals(Level.WARN, argument.getAllValues().get(1).getLevel());
            assertTrue(argument.getAllValues().get(1).getRenderedMessage().contains("Change of accesslog file path not allowed"));
        } finally {
            customAccessLogFilter.destroy();
            realLogger.removeAppender(appender);
            AccessLogFilter.logger = originalLogger;
            AccessLogFilter.setInterval(originalInterval);
        }

        Appender appender2 = mock(Appender.class);
        AccessLogFilter.setInterval(500);
        AccessLogFilter.logger = logger;
        AccessLogFilter customAccessLogFilter2 = new AccessLogFilter();
        try {
            realLogger.addAppender(appender2);
            URL url2 = URL.valueOf("test://test:11/test?accesslog=custom-access.log&accesslog.fixed.path=false");
            Invoker<AccessLogFilterTest> invoker = new MyInvoker<>(url2);
            Invocation invocation = new MockInvocation();
            customAccessLogFilter2.invoke(invoker, invocation);
            sleep();
            ArgumentCaptor<LoggingEvent> argument = ArgumentCaptor.forClass(LoggingEvent.class);
            verify(appender2, times(2)).doAppend(argument.capture());
            assertEquals(Level.WARN, argument.getAllValues().get(1).getLevel());
            assertTrue(argument.getAllValues().get(1).getRenderedMessage().contains("Accesslog file path changed to"));
        } finally {
            customAccessLogFilter2.destroy();
            realLogger.removeAppender(appender2);
            AccessLogFilter.logger = originalLogger;
            AccessLogFilter.setInterval(originalInterval);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
