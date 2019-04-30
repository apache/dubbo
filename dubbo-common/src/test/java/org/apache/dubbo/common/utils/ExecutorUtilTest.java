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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExecutorUtilTest {
    @Test
    public void testIsTerminated() throws Exception {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        when(executor.isTerminated()).thenReturn(true);
        assertThat(ExecutorUtil.isTerminated(executor), is(true));
        Executor executor2 = Mockito.mock(Executor.class);
        assertThat(ExecutorUtil.isTerminated(executor2), is(false));
    }

    @Test
    public void testGracefulShutdown1() throws Exception {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        when(executor.isTerminated()).thenReturn(false, true);
        when(executor.awaitTermination(20, TimeUnit.MILLISECONDS)).thenReturn(false);
        ExecutorUtil.gracefulShutdown(executor, 20);
        verify(executor).shutdown();
        verify(executor).shutdownNow();
    }

    @Test
    public void testGracefulShutdown2() throws Exception {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        when(executor.isTerminated()).thenReturn(false, false, false);
        when(executor.awaitTermination(20, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(executor.awaitTermination(10, TimeUnit.MILLISECONDS)).thenReturn(false, true);
        ExecutorUtil.gracefulShutdown(executor, 20);
        Thread.sleep(2000);
        verify(executor).shutdown();
        verify(executor, atLeast(2)).shutdownNow();
    }

    @Test
    public void testShutdownNow() throws Exception {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        when(executor.isTerminated()).thenReturn(false, true);
        ExecutorUtil.shutdownNow(executor, 20);
        verify(executor).shutdownNow();
        verify(executor).awaitTermination(20, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSetThreadName() throws Exception {
        URL url = new URL("dubbo", "localhost", 1234).addParameter(Constants.THREAD_NAME_KEY, "custom-thread");
        url = ExecutorUtil.setThreadName(url, "default-name");
        assertThat(url.getParameter(Constants.THREAD_NAME_KEY), equalTo("custom-thread-localhost:1234"));
    }
}
