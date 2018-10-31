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
package com.alibaba.dubbo.rpc.protol.rest;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.StaticContext;

import junit.framework.Assert;
import org.junit.Test;

/**
 * RestProtocolTest
 */
public class RestProtocolTest {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testRestProtocol() {
        URL url = URL.valueOf("rest://127.0.0.1:5342/rest/say1?version=1.0.0");
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(url.getServiceKey(), RestServiceImpl.class);
        RestServiceImpl server = new RestServiceImpl();
        Assert.assertFalse(server.isCalled());

        Exporter<RestService> exporter = protocol.export(proxyFactory.getInvoker(server, RestService.class, url));
        Invoker<RestService> invoker = protocol.refer(RestService.class, url);
        RestService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assert.assertTrue(server.isCalled());
        Assert.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }
}
