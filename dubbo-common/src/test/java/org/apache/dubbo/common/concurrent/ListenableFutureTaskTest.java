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
package org.apache.dubbo.common.concurrent;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ListenableFutureTaskTest {
    @Test
    public void testCreate() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ListenableFutureTask<Boolean> futureTask = ListenableFutureTask.create(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                countDownLatch.countDown();
                return true;
            }
        });
        futureTask.run();
        countDownLatch.await();
    }

    @Test
    public void testRunnableResponse() throws ExecutionException, InterruptedException {
        ListenableFutureTask<Boolean> futureTask = ListenableFutureTask.create(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, true);
        futureTask.run();

        Boolean result = futureTask.get();
        assertThat(result, is(true));
    }

    @Test
    public void testListener() throws InterruptedException {
        ListenableFutureTask<String> futureTask = ListenableFutureTask.create(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "hello";
            }
        });
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        futureTask.addListener(new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        });
        futureTask.run();
        countDownLatch.await();
    }


    @Test
    public void testCustomExecutor() {
        Executor mockedExecutor = mock(Executor.class);
        ListenableFutureTask<Integer> futureTask = ListenableFutureTask.create(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 0;
            }
        });
        futureTask.addListener(mock(Runnable.class), mockedExecutor);
        futureTask.run();

        verify(mockedExecutor).execute(any(Runnable.class));
    }
}