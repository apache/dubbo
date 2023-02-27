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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.DemoServiceImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

class StubInvokerTest {

    private final URL url = URL.valueOf("tri://0.0.0.0:50051/" + DemoService.class.getName());
    private final String methodName = "sayHello";
    private final BiConsumer<String, StreamObserver<String>> func = (a, o) -> {
        o.onNext("hello," + a);
        o.onCompleted();
    };
    private final Map<String, StubMethodHandler<?, ?>> methodMap = Collections.singletonMap(
        methodName, new UnaryStubMethodHandler<>(func));
    private final StubInvoker<DemoService> invoker = new StubInvoker<>(new DemoServiceImpl(), url,
        DemoService.class, methodMap);

    @Test
    void getUrl() {
        Assertions.assertEquals(url, invoker.getUrl());
    }

    @Test
    void isAvailable() {
        Assertions.assertTrue(invoker.isAvailable());
    }

    @Test
    void destroy() {
        invoker.destroy();
    }

    @Test
    void getInterface() {
        Assertions.assertEquals(DemoService.class, invoker.getInterface());
    }

    @Test
    void invoke() {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(methodName);
        invocation.setArguments(new Object[]{"test"});
        Result result = invoker.invoke(invocation);
        Object value = result.getValue();
        Assertions.assertEquals("hello,test", value);
    }
}
