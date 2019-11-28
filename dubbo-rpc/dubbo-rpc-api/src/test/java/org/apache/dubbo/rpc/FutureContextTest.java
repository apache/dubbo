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
package org.apache.dubbo.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/**
 *
 */
public class FutureContextTest {

    @Test
    public void testFutureContext() throws Exception {
        Thread thread1 = new Thread(() -> {
            FutureContext.getContext().setFuture(CompletableFuture.completedFuture("future from thread1"));
            try {
                Thread.sleep(500);
                Assertions.assertEquals("future from thread1", FutureContext.getContext().getCompletableFuture().get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread1.start();

        Thread.sleep(100);

        Thread thread2 = new Thread(() -> {
            CompletableFuture future = FutureContext.getContext().getCompletableFuture();
            Assertions.assertNull(future);
            FutureContext.getContext().setFuture(CompletableFuture.completedFuture("future from thread2"));
        });
        thread2.start();

        Thread.sleep(1000);
    }
}
