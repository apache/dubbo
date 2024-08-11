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
package org.apache.dubbo.demo;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreeterWrapperServiceImpl implements GreeterWrapperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GreeterWrapperServiceImpl.class);

    @Override
    public String sayHello(String request) {
        LOGGER.info("Received sayHello request: {}", request);
        return toReply(request);
    }

    @Override
    public CompletableFuture<String> sayHelloAsync(String name) {
        LOGGER.info("Received sayHelloAsync request: {}", name);
        return CompletableFuture.supplyAsync(() -> toReply(name));
    }

    @Override
    public void sayHelloServerStream(String request, StreamObserver<String> responseObserver) {
        LOGGER.info("Received sayHelloServerStream request");
        for (int i = 1; i < 6; i++) {
            LOGGER.info("sayHelloServerStream onNext: {} {} times", request, i);
            responseObserver.onNext(toReply(request + ' ' + i + " times"));
        }
        LOGGER.info("sayHelloServerStream onCompleted");
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<String> sayHelloBiStream(StreamObserver<String> responseObserver) {
        LOGGER.info("Received sayHelloBiStream request");
        return new StreamObserver<String>() {
            @Override
            public void onNext(String request) {
                LOGGER.info("sayHelloBiStream onNext: {}", request);
                responseObserver.onNext(toReply(request));
                responseObserver.onError(new HttpResultPayloadException(HttpResult.error("Error " + request)));
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("sayHelloBiStream onError", throwable);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("sayHelloBiStream onCompleted");
            }
        };
    }

    private static String toReply(String message) {
        return "Hello " + message;
    }
}
