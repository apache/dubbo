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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class BiStreamMethodHandlerTest {

    @Test
    void invoke() throws ExecutionException, InterruptedException, TimeoutException {
        StreamObserver<String> responseObserver = Mockito.mock(StreamObserver.class);
        BiStreamMethodHandler<String, String> handler = new BiStreamMethodHandler<>(
            o -> responseObserver);
        CompletableFuture<StreamObserver<String>> future = handler.invoke(
            new Object[]{responseObserver});
        Assertions.assertEquals(responseObserver, future.get(1, TimeUnit.SECONDS));
    }
}
