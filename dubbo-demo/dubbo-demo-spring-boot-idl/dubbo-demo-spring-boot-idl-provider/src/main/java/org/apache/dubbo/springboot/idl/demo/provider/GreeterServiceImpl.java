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

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.demo.hello.GreeterService;
import org.apache.dubbo.demo.hello.HelloReply;
import org.apache.dubbo.demo.hello.HelloRequest;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DubboService
public class GreeterServiceImpl implements GreeterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GreeterServiceImpl.class);

    @Override
    public HelloReply sayHello(HelloRequest request) {
        LOGGER.info("Received sayHello request: {}", request.getName());
        return toReply("Hello " + request.getName());
    }

    @Override
    public CompletableFuture<HelloReply> sayHelloAsync(HelloRequest request) {
        LOGGER.info("Received sayHelloAsync request: {}", request.getName());
        HelloReply.newBuilder().setMessage("Hello " + request.getName());
        return CompletableFuture.supplyAsync(() ->
                HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
    }

    private static HelloReply toReply(String message) {
        return HelloReply.newBuilder().setMessage(message).build();
    }
}
