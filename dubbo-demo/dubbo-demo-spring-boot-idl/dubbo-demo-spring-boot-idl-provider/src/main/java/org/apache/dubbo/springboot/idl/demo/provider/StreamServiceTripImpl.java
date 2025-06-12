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
package org.apache.dubbo.springboot.idl.demo.provider;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.demo.stream.DubboStreamServiceTriple.StreamServiceImplBase;
import org.apache.dubbo.demo.stream.HelloReply;
import org.apache.dubbo.demo.stream.HelloRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DubboService
public class StreamServiceTripImpl extends StreamServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamServiceTripImpl.class);

    @Override
    public HelloReply sayHello(HelloRequest request) {
        LOGGER.info("receive sayHello request: " + request.getName());
        return HelloReply.newBuilder().setMessage(request.getName()).build();
    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        LOGGER.info("receive sayHello request: " + request.getName() + ", will response by async");
        super.sayHello(request, responseObserver);
    }

    @Override
    public CompletableFuture<HelloReply> sayHelloAsync(HelloRequest request) {
        LOGGER.info("receive sayHelloAsync request: " + request.getName() + ", will response by async");
        return CompletableFuture.completedFuture(sayHello(request));
    }

    @Override
    public void sayServerStream(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        for (int i = 0; i < 10; i++) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                responseObserver.onError(e);
                e.printStackTrace();
            }
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LOGGER.info("sayServerStream response every 2 s, data: " + now);
            responseObserver.onNext(HelloReply.newBuilder().setMessage(now).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<HelloRequest> sayClientStream(StreamObserver<HelloReply> responseObserver) {
        return new StreamObserver<HelloRequest>() {
            final StringBuilder sb = new StringBuilder();

            @Override
            public void onNext(HelloRequest data) {
                LOGGER.info("sayClientStream receive request: " + data.getName());
                sb.append("data -> ").append(data.getName()).append("\n");
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                String reply = sb.toString();
                LOGGER.info("sayClientStream client send all the data, response now : " + reply);
                responseObserver.onNext(HelloReply.newBuilder()
                        .setMessage("the full data: \n" + reply)
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<HelloRequest> sayBIStream(StreamObserver<HelloReply> responseObserver) {
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest data) {
                LOGGER.info("sayBIStream receive request: " + data.getName() + ", and response data");
                responseObserver.onNext(HelloReply.newBuilder()
                        .setMessage("receive: " + data.getName())
                        .build());
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("something error, errMsg: " + throwable.getMessage(), throwable);
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("the stream has been closed!");
                responseObserver.onCompleted();
            }
        };
    }
}
