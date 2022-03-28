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
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.DemoServiceImpl;
import org.apache.dubbo.rpc.support.MyInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


public abstract class AbstractProxyTest {

    public static ProxyFactory factory;

    @Test
    public void testGetProxy() throws Exception {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");

        MyInvoker<DemoService> invoker = new MyInvoker<>(url);

        DemoService proxy = factory.getProxy(invoker);

        Assertions.assertNotNull(proxy);

        Assertions.assertTrue(Arrays.asList(proxy.getClass().getInterfaces()).contains(DemoService.class));
        Assertions.assertTrue(Arrays.asList(proxy.getClass().getInterfaces()).contains(Destroyable.class));
        Assertions.assertTrue(Arrays.asList(proxy.getClass().getInterfaces()).contains(EchoService.class));

        Assertions.assertEquals(invoker.invoke(new RpcInvocation("echo", DemoService.class.getName(), DemoService.class.getName() + ":dubbo", new Class[]{String.class}, new Object[]{"aa"})).getValue()
                , proxy.echo("aa"));

        Destroyable destroyable = (Destroyable)proxy;
        destroyable.$destroy();
        Assertions.assertTrue(invoker.isDestroyed());

    }

    @Test
    public void testGetInvoker() throws Exception {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");

        DemoService origin = new org.apache.dubbo.rpc.support.DemoServiceImpl();

        Invoker<DemoService> invoker = factory.getInvoker(new DemoServiceImpl(), DemoService.class, url);

        Assertions.assertEquals(invoker.getInterface(), DemoService.class);

        Assertions.assertEquals(invoker.invoke(new RpcInvocation("echo", DemoService.class.getName(), DemoService.class.getName() + ":dubbo", new Class[]{String.class}, new Object[]{"aa"})).getValue(),
                origin.echo("aa"));

    }

}
