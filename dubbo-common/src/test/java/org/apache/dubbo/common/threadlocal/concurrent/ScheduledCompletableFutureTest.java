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
package org.apache.dubbo.common.threadlocal.concurrent;

import org.apache.dubbo.common.threadpool.concurrent.ScheduledCompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

public class ScheduledCompletableFutureTest {
    @Test
    public void testSubmit() throws Exception {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        CompletableFuture<String> futureOK = ScheduledCompletableFuture.submit(executorService, () -> "done");
        Assertions.assertEquals(futureOK.get(), "done");

        CompletableFuture<Integer> futureFail = ScheduledCompletableFuture.submit(executorService, () -> 1 / 0);
        Assertions.assertThrows(ExecutionException.class, () -> futureFail.get());


    }

    @Test
    public void testSchedule() throws Exception {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        CompletableFuture<String> futureOK = ScheduledCompletableFuture.schedule(executorService, () -> "done", 1000, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(futureOK.get(), "done");

        CompletableFuture<Integer> futureFail = ScheduledCompletableFuture.schedule(executorService, () -> 1 / 0, 1000, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(ExecutionException.class, () -> futureFail.get());

    }
}