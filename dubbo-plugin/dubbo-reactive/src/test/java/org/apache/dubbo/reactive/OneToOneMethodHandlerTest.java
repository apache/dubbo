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

package org.apache.dubbo.reactive;

import org.apache.dubbo.reactive.handler.OneToOneMethodHandler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for OneToOneMethodHandler
 */
public final class OneToOneMethodHandlerTest {

    @Test
    void testInvoke() throws ExecutionException, InterruptedException {
        String request = "request";
        OneToOneMethodHandler<String, String> handler = new OneToOneMethodHandler<>(requestMono ->
            requestMono.map(r -> r + "Test"));
        CompletableFuture<?> future = handler.invoke(new Object[]{request});
        assertEquals("requestTest", future.get());
    }
}
