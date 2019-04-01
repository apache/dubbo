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

package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.ReactiveDemoService;
import org.apache.dubbo.rpc.support.ReactiveDemoServiceImpl;
import org.apache.dubbo.rpc.support.MyReactiveInvoker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.Constants.*;


public abstract class AbstractReactiveProxyTest {

    protected ProxyFactory factory;

    @Test
    public void testGetProxy() throws Exception {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");

        Invoker<ReactiveDemoService> invoker = new MyReactiveInvoker<>(url);

        ReactiveDemoService proxy = factory.getProxy(invoker);

        Assertions.assertNotNull(proxy);

        Assertions.assertTrue(Arrays.asList(proxy.getClass().getInterfaces()).contains(ReactiveDemoService.class));

        RpcInvocation invocation = new RpcInvocation("sayHello", new Class[]{String.class}, new Object[]{"aa"});
        invocation.setAttachment(KEY_PUBLISHER_TYPE,VALUE_PUBLISHER_MONO);
        try {
            Object retVal = invoker.invoke(invocation).recreate();
            Mono mono = Mono.fromFuture((CompletableFuture<?>) retVal);
            StepVerifier
                    .create(mono)
                    .expectNext("Hello aa")
                    .expectComplete();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Test
    public void testGetInvoker() throws Exception {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");

        ReactiveDemoService origin = new ReactiveDemoServiceImpl();

        Invoker<ReactiveDemoService> invoker = factory.getInvoker(new ReactiveDemoServiceImpl(), ReactiveDemoService.class, url);

        Assertions.assertEquals(invoker.getInterface(), ReactiveDemoService.class);

        RpcInvocation invocation = new RpcInvocation("sayHello", new Class[]{String.class}, new Object[]{"aa"});
        invocation.setAttachment(KEY_PUBLISHER_TYPE,VALUE_PUBLISHER_MONO);
        StepVerifier
                .create(origin.sayHello("aa"))
                .expectNext("Hello aa")
                .expectComplete();

        try {
            Mono mono = Mono.fromFuture((CompletableFuture<?>) invoker.invoke(invocation).recreate());

            StepVerifier
                    .create(mono)
                    .expectNext("Hello aa")
                    .expectComplete();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
