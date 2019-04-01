/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.demo.ReactiveDemoService;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

//specify a reactive proxy factory
@Service(proxy = "reactivejavassist")
public class ReactiveDemoServiceImpl implements ReactiveDemoService {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveDemoServiceImpl.class);

    @Override
    public Mono<String> sayHello(String name) {
        return Mono.create(sink -> {
            logger.info("Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
            sink.success("Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress());
        });
    }

    @Override
    public Mono<List<String>> sayHellos(String name) {
        return Mono.create(sink -> {
            logger.info("Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
            sink.success(Arrays
                    .asList("Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress(),
                            "Aloha " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress()));
        });
    }

    @Override
    public Flux<String> namesList() {
        return Flux.create(sink -> {
            logger.info("request from consumer: " + RpcContext.getContext().getRemoteAddress());
            sink.next("name1");
            sink.next("name2");
            sink.next("name3");
            sink.complete();
        });
    }

    @Override
    public Flux<List<String>> namesMatrix() {
        return Flux.create(sink -> {
            logger.info("request from consumer: " + RpcContext.getContext().getRemoteAddress());
            sink.next(Arrays.asList("name1","name2","name3"));
            sink.next(Arrays.asList("name4","name5"));
            sink.complete();
        });
    }

}
