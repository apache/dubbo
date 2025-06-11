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

package org.apache.dubbo.rpc.protocol.tri.proto;

import org.apache.dubbo.common.stream.StreamObserver;
import java.util.concurrent.CompletableFuture;

public class DemoServiceImpl extends DubboDemoServiceTriple.DemoServiceImplBase {

    @Override
    public HelloResponse helloUnary(HelloRequest request) {
        return super.helloUnary(request);
    }

    @Override
    public void helloUnary(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        super.helloUnary(request, responseObserver);
    }

    @Override
    public CompletableFuture<HelloResponse> helloUnaryAsync(HelloRequest request) {
        return super.helloUnaryAsync(request);
    }

    @Override
    public StreamObserver<HelloRequest> helloClientStream(StreamObserver<HelloResponse> responseObserver) {
        return super.helloClientStream(responseObserver);
    }

    @Override
    public void helloServerStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        super.helloServerStream(request, responseObserver);
    }

    @Override
    public void helloBIStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        super.helloBIStream(request, responseObserver);
    }
}
