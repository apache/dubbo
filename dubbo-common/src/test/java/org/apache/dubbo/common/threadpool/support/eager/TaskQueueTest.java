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

package org.apache.dubbo.common.threadpool.support.eager;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class TaskQueueTest {

    @Test(expected = RejectedExecutionException.class)
    public void testOffer1() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        queue.offer(mock(Runnable.class));
    }

    @Test
    public void testOffer2() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.getPoolSize()).thenReturn(2);
        Mockito.when(executor.getSubmittedTaskCount()).thenReturn(1);
        queue.setExecutor(executor);
        assertThat(queue.offer(mock(Runnable.class)), is(true));
    }

    @Test
    public void testOffer3() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.getPoolSize()).thenReturn(2);
        Mockito.when(executor.getSubmittedTaskCount()).thenReturn(2);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(4);
        queue.setExecutor(executor);
        assertThat(queue.offer(mock(Runnable.class)), is(false));
    }

    @Test
    public void testOffer4() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.getPoolSize()).thenReturn(4);
        Mockito.when(executor.getSubmittedTaskCount()).thenReturn(4);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(4);
        queue.setExecutor(executor);
        assertThat(queue.offer(mock(Runnable.class)), is(true));
    }

    @Test(expected = RejectedExecutionException.class)
    public void testRetryOffer1() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.isShutdown()).thenReturn(true);
        queue.setExecutor(executor);
        queue.retryOffer(mock(Runnable.class), 1000, TimeUnit.MILLISECONDS);
    }


    @Test
    public void testRetryOffer2() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.isShutdown()).thenReturn(false);
        queue.setExecutor(executor);
        assertThat(queue.retryOffer(mock(Runnable.class), 1000, TimeUnit.MILLISECONDS), is(true));
    }

}
