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

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;

import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class DeadlineFutureTest {

    @Test
    void test() throws InterruptedException, ExecutionException {
        String service = "service";
        String method = "method";
        String address = "localhost:12201";
        DeadlineFuture timeout = DeadlineFuture.newFuture(service, method, address, 10,
            ImmediateEventExecutor.INSTANCE);
        TimeUnit.MILLISECONDS.sleep(20);
        AppResponse timeoutResponse = timeout.get();
        Assertions.assertTrue(timeoutResponse.getException() instanceof StatusRpcException);


        DeadlineFuture success = DeadlineFuture.newFuture(service, method, address, 1000,
            ImmediateEventExecutor.INSTANCE);
        AppResponse response = new AppResponse();
        success.received(TriRpcStatus.OK, response);
        AppResponse response1 = success.get();
        Assertions.assertEquals(response, response1);
    }
}
