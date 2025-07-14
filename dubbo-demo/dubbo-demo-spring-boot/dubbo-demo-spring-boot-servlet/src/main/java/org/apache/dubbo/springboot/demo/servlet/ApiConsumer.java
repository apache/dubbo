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
package org.apache.dubbo.springboot.demo.servlet;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ApiConsumer.class);

    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<GreeterService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterService.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol(CommonConstants.TRIPLE);
        referenceConfig.setLazy(true);
        referenceConfig.setTimeout(100000);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("dubbo-demo-triple-api-consumer"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
                .reference(referenceConfig)
                .start();

        GreeterService greeterService = referenceConfig.get();
        logger.info("dubbo referenceConfig started");
        logger.info("Call sayHello");
        HelloReply reply = greeterService.sayHello(buildRequest("triple"));
        logger.info("sayHello reply: {}", reply.getMessage());

        logger.info("Call sayHelloAsync");
        CompletableFuture<String> sayHelloAsync = greeterService.sayHelloAsync("triple");
        sayHelloAsync.thenAccept(value -> logger.info("sayHelloAsync reply: {}", value));

        StreamObserver<HelloReply> responseObserver = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply reply) {
                logger.info("sayHelloServerStream onNext: {}", reply.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                logger.info("sayHelloServerStream onError: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("sayHelloServerStream onCompleted");
            }
        };
        logger.info("Call sayHelloServerStream");
        greeterService.sayHelloServerStream(buildRequest("triple"), responseObserver);

        StreamObserver<HelloReply> sayHelloServerStreamNoParameterResponseObserver = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply reply) {
                logger.info("sayHelloServerStreamNoParameter onNext: {}", reply.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                logger.info("sayHelloServerStreamNoParameter onError: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("sayHelloServerStreamNoParameter onCompleted");
            }
        };

        greeterService.sayHelloServerStreamNoParameter(sayHelloServerStreamNoParameterResponseObserver);

        StreamObserver<HelloReply> biResponseObserver = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply reply) {
                logger.info("biRequestObserver onNext: {}", reply.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                logger.info("biResponseObserver onError: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("biResponseObserver onCompleted");
            }
        };
        logger.info("Call biRequestObserver");
        StreamObserver<HelloRequest> biRequestObserver = greeterService.sayHelloBiStream(biResponseObserver);
        for (int i = 0; i < 5; i++) {
            biRequestObserver.onNext(buildRequest("triple" + i));
        }
        biRequestObserver.onCompleted();

        Thread.sleep(2000);
    }

    private static HelloRequest buildRequest(String name) {
        HelloRequest request = new HelloRequest();
        request.setName(name);
        return request;
    }
}
