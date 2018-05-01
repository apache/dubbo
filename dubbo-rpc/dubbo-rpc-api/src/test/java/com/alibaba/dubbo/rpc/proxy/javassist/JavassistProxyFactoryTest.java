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
package com.alibaba.dubbo.rpc.proxy.javassist;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.support.DemoService;
import com.alibaba.dubbo.rpc.support.MyInvoker;
import org.junit.Assert;
import org.junit.Test;

public class JavassistProxyFactoryTest {

    private JavassistProxyFactory javassistProxyFactory = new JavassistProxyFactory();

    @Test
    public void testGetProxy() throws Exception {
        URL url = URL.valueOf("test://test:11/test?group=dubbo&version=1.1");

        DemoService proxy = javassistProxyFactory.getProxy(new MyInvoker<DemoService>(url), new Class[]{DemoService.class});

        Assert.assertNotNull(proxy);

        Invoker<DemoService> invoker = javassistProxyFactory.getInvoker(proxy, DemoService.class, url);

        Class<?>[] classes = invoker.getClass().getInterfaces();

    }

}
