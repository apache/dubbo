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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedThreadFactoryTest {

    private static final int INITIAL_THREAD_NUM = 1;

    @Test
    void testNewThread() {
        NamedThreadFactory factory = new NamedThreadFactory();
        Thread t = factory.newThread(Mockito.mock(Runnable.class));
        assertThat(t.getName(), allOf(containsString("pool-"), containsString("-thread-")));
        assertFalse(t.isDaemon());
        // since security manager is not installed.
        assertSame(t.getThreadGroup(), Thread.currentThread().getThreadGroup());
    }

    @Test
    void testPrefixAndDaemon() {
        NamedThreadFactory factory = new NamedThreadFactory("prefix", true);
        Thread t = factory.newThread(Mockito.mock(Runnable.class));
        assertThat(t.getName(), allOf(containsString("prefix-"), containsString("-thread-")));
        assertTrue(t.isDaemon());
    }

    @Test
    public void testGetThreadNum() {
        NamedThreadFactory threadFactory = new NamedThreadFactory();
        AtomicInteger threadNum = threadFactory.getThreadNum();
        assertNotNull(threadNum);
        assertEquals(INITIAL_THREAD_NUM, threadNum.get());
    }

    @Test
    public void testGetThreadGroup() {
        NamedThreadFactory threadFactory = new NamedThreadFactory();
        ThreadGroup threadGroup = threadFactory.getThreadGroup();
        assertNotNull(threadGroup);
    }
}
