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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.rpc.RpcContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * ReactiveDemoServiceImpl
 */

public class ReactiveDemoServiceImpl implements ReactiveDemoService {

    @Override
    public Mono<String> sayHello(String name) {
        return Mono.create(sink -> {
            sink.success("Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress());
        });
    }

    @Override
    public Mono<List<String>> sayHellos(String name) {
        return Mono.create(sink -> {
            sink.success(Arrays
                    .asList("Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress(),
                            "Aloha " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress()));
        });
    }

    @Override
    public Flux<String> namesList() {
        return Flux.create(sink -> {
            sink.next("name1");
            sink.next("name2");
            sink.next("name3");
            sink.complete();
        });
    }

    @Override
    public Flux<List<String>> namesMatrix() {
        return Flux.create(sink -> {
            sink.next(Arrays.asList("name1","name2","name3"));
            sink.next(Arrays.asList("name4","name5"));
            sink.complete();
        });
    }
}