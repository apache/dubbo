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

import com.alibaba.dubbo.rpc.RpcContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RpcContextTest {

    @Test
    public void testSetFuture() {
        CompletableFuture completableFuture = new CompletableFuture();
        RpcContext.getContext().setFuture(completableFuture);

        CompletableFuture result = FutureContext.getContext().getCompatibleCompletableFuture();
        assertEquals(result, completableFuture);
    }

    @Test
    public void testSetFutureAlibaba() {
        CompletableFuture completableFuture = new CompletableFuture();
        RpcContext.getContext().setFuture(completableFuture);

        Future future = RpcContext.getContext().getFuture();

        System.out.println(future);
    }
}
