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
package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ThreadlessExecutorTest {
    private static ThreadlessExecutor executor;

    static {
        URL url = URL.valueOf("dubbo://127.0.0.1:12345");
        ExecutorService sharedExecutor =
                ExtensionLoader.getExtensionLoader(ExecutorRepository.class)
                        .getDefaultExtension().createExecutorIfAbsent(url);
        executor = new ThreadlessExecutor(sharedExecutor);
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            executor.execute(()->{throw new RuntimeException("test");});
        }

        CompletableFuture<Object> stubFuture = new CompletableFuture<>();
        executor.setWaitingFuture(stubFuture);
        Assertions.assertEquals(executor.getWaitingFuture(),stubFuture);

        executor.waitAndDrain();

        executor.execute(()->{});

        executor.waitAndDrain();

        executor.shutdown();
    }
}
