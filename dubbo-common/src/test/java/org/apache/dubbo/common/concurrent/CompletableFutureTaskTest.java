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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.junit.jupiter.api.Test;


public class CompletableFutureTaskTest {

    private static final ExecutorService executor = new ThreadPoolExecutor(0, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("DubboMonitorCreator", true));

    @Test
    public void testCreate() throws InterruptedException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(() -> {
            countDownLatch.countDown();
            return true;
        }, executor);
        countDownLatch.await();
    }

    @Test
    public void testRunnableResponse() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }, executor);

        Boolean result = completableFuture.get();
        assertThat(result, is(true));
    }

    @Test
    public void testListener() throws InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "hello";

        }, executor);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        completableFuture.thenRunAsync(countDownLatch::countDown);
        countDownLatch.await();
    }


@Test
    public void testCustomExecutor() {
        Executor mockedExecutor = mock(Executor.class);
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            return 0;
        });
        completableFuture.thenRunAsync(mock(Runnable.class), mockedExecutor).whenComplete((s, e) ->
                verify(mockedExecutor, times(1)).execute(any(Runnable.class)));
    }
}