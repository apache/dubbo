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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DeadlineFutureTest {

    @Test
    void responseAfterSynchronousWaiterExitDoesNotLoseCompletion() {
        ThreadlessExecutor executor = new ThreadlessExecutor();
        DeadlineFuture future = DeadlineFuture.newFuture("service", "method", "address", 1000, executor);
        executor.shutdown();
        AppResponse response = new AppResponse("late");
        future.received(TriRpcStatus.OK, response);
        Assertions.assertEquals(response, future.join());
    }

    @Test
    void timeoutStillCancelsTransportAfterSynchronousWaiterHasExited() throws Exception {
        ThreadlessExecutor executor = new ThreadlessExecutor();
        DeadlineFuture future = DeadlineFuture.newFuture("service", "method", "address", 30, executor);
        CountDownLatch cancelled = new CountDownLatch(1);
        future.addTimeoutListener(cancelled::countDown);
        executor.shutdown();
        Assertions.assertTrue(cancelled.await(1, TimeUnit.SECONDS));
        Assertions.assertEquals(
                TriRpcStatus.Code.DEADLINE_EXCEEDED,
                TriRpcStatus.getStatus(future.get(1, TimeUnit.SECONDS).getException()).code);
    }

    @Test
    void explicitCancellationAndLateStartCancelEachListenerOnce() {
        DeadlineFuture future = DeadlineFuture.newFuture("service", "method", "address", 1000, null);
        AtomicInteger calls = new AtomicInteger();
        future.addTimeoutListener(calls::incrementAndGet);
        future.cancel(true);
        future.cancel(true);
        future.addTimeoutListener(calls::incrementAndGet);
        Assertions.assertEquals(2, calls.get());
        Assertions.assertEquals(
                TriRpcStatus.Code.CANCELLED,
                TriRpcStatus.getStatus(future.join().getException()).code);
    }

    @Test
    void test() throws InterruptedException, ExecutionException {
        String service = "service";
        String method = "method";
        String address = "localhost:12201";
        DeadlineFuture timeout =
                DeadlineFuture.newFuture(service, method, address, 10, ImmediateEventExecutor.INSTANCE);
        TimeUnit.MILLISECONDS.sleep(20);
        AppResponse timeoutResponse = timeout.get();
        Assertions.assertTrue(timeoutResponse.getException() instanceof StatusRpcException);

        DeadlineFuture success =
                DeadlineFuture.newFuture(service, method, address, 1000, ImmediateEventExecutor.INSTANCE);
        AppResponse response = new AppResponse();
        success.received(TriRpcStatus.OK, response);
        AppResponse response1 = success.get();
        Assertions.assertEquals(response, response1);
    }
}
