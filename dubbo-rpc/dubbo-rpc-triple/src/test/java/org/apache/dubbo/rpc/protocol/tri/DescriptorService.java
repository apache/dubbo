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

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;

public interface DescriptorService {


    CompletableFuture<String> unaryFuture();

    void noParameterMethod();

    /**
     * unray return protobuf class
     *
     * @return protobuf class
     */
    HelloReply noParameterAndReturnProtobufMethod();

    /**
     * unray return java class
     *
     * @return
     */
    String noParameterAndReturnJavaClassMethod();


    /**
     * bi stream need wrapper
     *
     * @param streamObserver
     * @return
     */
    StreamObserver<String> wrapBidirectionalStream(StreamObserver<String> streamObserver);

    /**
     * no need wrapper bi stream
     *
     * @param streamObserver
     * @return
     */
    StreamObserver<HelloReply> bidirectionalStream(StreamObserver<HelloReply> streamObserver);

    /**
     * only for test.
     *
     * @param reply
     * @return
     */
    HelloReply sayHello(HelloReply reply);

    void sayHelloServerStream(HelloReply request, StreamObserver<HelloReply> reply);

    void sayHelloServerStream2(Object request, StreamObserver<Object> reply);

    /***********************grpc******************************/

    java.util.Iterator<HelloReply> iteratorServerStream(HelloReply request);

    reactor.core.publisher.Mono<HelloReply> reactorMethod(HelloReply reactorRequest);

    reactor.core.publisher.Mono<HelloReply> reactorMethod2(reactor.core.publisher.Mono<HelloReply> reactorRequest);

    io.reactivex.Single<HelloReply> rxJavaMethod(io.reactivex.Single<HelloReply> replySingle);

    /**********************test error*****************/
    void testMultiProtobufParameters(HelloReply reply1, HelloReply reply2);

    String testDiffParametersAndReturn(HelloReply reply1);

    HelloReply testDiffParametersAndReturn2(String reply1);

    void testErrorServerStream(StreamObserver<HelloReply> reply, HelloReply request);

    void testErrorServerStream2(HelloReply request, HelloReply request2, StreamObserver<HelloReply> reply);

    void testErrorServerStream3(String aa, StreamObserver<HelloReply> reply);

    void testErrorServerStream4(String aa, String bb, StreamObserver<String> reply);

    StreamObserver<HelloReply> testErrorBiStream(HelloReply reply, StreamObserver<HelloReply> observer);

    StreamObserver<HelloReply> testErrorBiStream2(String reply, StreamObserver<HelloReply> observer);

    StreamObserver<String> testErrorBiStream3(StreamObserver<HelloReply> observer);

    StreamObserver<String> testErrorBiStream4(StreamObserver<HelloReply> observer, String str);


}
