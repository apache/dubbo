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
package com.alibaba.dubbo.common.concurrent;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ExecutionListTest {
    private ExecutionList executionList;

    @Before
    public void setUp() throws Exception {
        this.executionList = new ExecutionList();
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullRunnable() {
        this.executionList.add(null, mock(Executor.class));
    }

    @Test
    public void testAddRunnableToExecutor() {
        Executor mockedExecutor = mock(Executor.class);

        this.executionList.add(mock(Runnable.class), mockedExecutor);
        this.executionList.execute();

        verify(mockedExecutor).execute(any(Runnable.class));
    }

    @Test
    public void testExecuteRunnableWithDefaultExecutor() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        this.executionList.add(new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        }, null);

        this.executionList.execute();
        countDownLatch.await();
    }

    @Test
    public void testExceptionForExecutor() {
        Executor mockedExecutor = mock(Executor.class);
        doThrow(new RuntimeException()).when(mockedExecutor).execute(any(Runnable.class));

        this.executionList.add(mock(Runnable.class), mockedExecutor);
        this.executionList.execute();
    }

    @Test
    public void testNotRunSameRunnableTwice() {
        Executor mockedExecutor = mock(Executor.class);

        this.executionList.add(mock(Runnable.class), mockedExecutor);

        this.executionList.execute();
        this.executionList.execute();

        verify(mockedExecutor).execute(any(Runnable.class));
    }

    @Test
    public void testRunImmediatelyAfterExecuted() {
        Executor mockedExecutor = mock(Executor.class);

        this.executionList.add(mock(Runnable.class), mockedExecutor);
        this.executionList.execute();
        this.executionList.add(mock(Runnable.class), mockedExecutor);

        verify(mockedExecutor, times(2)).execute(any(Runnable.class));
    }
}