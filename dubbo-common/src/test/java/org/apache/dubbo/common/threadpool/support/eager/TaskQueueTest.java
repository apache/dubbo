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
package org.apache.dubbo.common.threadpool.support.eager;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

class TaskQueueTest {

    private TaskQueue<Runnable> queue;
    private EagerThreadPoolExecutor executor;

    @BeforeEach
    void setup() {
        queue = new TaskQueue<Runnable>(1);
        executor = mock(EagerThreadPoolExecutor.class);
        queue.setExecutor(executor);
    }

    @Test
    void testOffer1() throws Exception {
        Assertions.assertThrows(RejectedExecutionException.class, () -> {
            TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
            queue.offer(mock(Runnable.class));
        });
    }

    @Test
    void testOffer2() throws Exception {
        Mockito.when(executor.getPoolSize()).thenReturn(2);
        Mockito.when(executor.getActiveCount()).thenReturn(1);
        assertThat(queue.offer(mock(Runnable.class)), is(true));
    }

    @Test
    void testOffer3() throws Exception {
        Mockito.when(executor.getPoolSize()).thenReturn(2);
        Mockito.when(executor.getActiveCount()).thenReturn(2);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(4);
        assertThat(queue.offer(mock(Runnable.class)), is(false));
    }

    @Test
    void testOffer4() throws Exception {
        Mockito.when(executor.getPoolSize()).thenReturn(4);
        Mockito.when(executor.getActiveCount()).thenReturn(4);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(4);
        assertThat(queue.offer(mock(Runnable.class)), is(true));
    }

    @Test
    void testRetryOffer1() throws Exception {
        Assertions.assertThrows(RejectedExecutionException.class, () -> {
            Mockito.when(executor.isShutdown()).thenReturn(true);
            queue.retryOffer(mock(Runnable.class), 1000, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    void testRetryOffer2() throws Exception {
        Mockito.when(executor.isShutdown()).thenReturn(false);
        assertThat(queue.retryOffer(mock(Runnable.class), 1000, TimeUnit.MILLISECONDS), is(true));
    }
}
