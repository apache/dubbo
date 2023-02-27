/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.utils;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DubboAppenderTest {
    private LoggingEvent event;

    @BeforeEach
    public void setUp() throws Exception {
        event = mock(LoggingEvent.class);
        when(event.getLogger()).thenReturn(mock(Category.class));
        when(event.getLevel()).thenReturn(mock(Level.class));
        when(event.getThreadName()).thenReturn("thread-name");
        when(event.getMessage()).thenReturn("message");
    }

    @AfterEach
    public void tearDown() throws Exception {
        DubboAppender.clear();
        DubboAppender.doStop();
    }

    @Test
    void testAvailable() {
        assumeFalse(DubboAppender.available);
        DubboAppender.doStart();
        assertThat(DubboAppender.available, is(true));
        DubboAppender.doStop();
        assertThat(DubboAppender.available, is(false));
    }

    @Test
    void testAppend() {
        DubboAppender appender = new DubboAppender();
        appender.append(event);
        assumeTrue(0 == DubboAppender.logList.size());
        DubboAppender.doStart();
        appender.append(event);
        assertThat(DubboAppender.logList, hasSize(1));
        assertThat(DubboAppender.logList.get(0).getLogThread(), equalTo("thread-name"));
    }

    @Test
    void testClear() {
        DubboAppender.doStart();
        DubboAppender appender = new DubboAppender();
        appender.append(event);
        assumeTrue(1 == DubboAppender.logList.size());
        DubboAppender.clear();
        assertThat(DubboAppender.logList, hasSize(0));
    }
}
