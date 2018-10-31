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
package com.alibaba.dubbo.rpc.protocol.rest;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.StaticContext;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RestProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final int availablePort = NetUtils.getAvailablePort();
    private final URL exportUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest");

    @After
    public void tearDown() {
        protocol.destroy();
    }

    @Test
    public void testExport() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);


        RpcContext.getContext().setAttachment("timeout", "200");
        Exporter<IDemoService> exporter = protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, exportUrl));

        IDemoService demoService = this.proxy.getProxy(protocol.refer(IDemoService.class, exportUrl));

        Integer echoString = demoService.hello(1, 2);
        assertThat(echoString, is(3));

        exporter.unexport();
    }

    @Test
    public void testNettyServer() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);

        URL nettyUrl = exportUrl.addParameter(Constants.SERVER_KEY, "netty");
        Exporter<IDemoService> exporter = protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, nettyUrl));

        IDemoService demoService = this.proxy.getProxy(protocol.refer(IDemoService.class, nettyUrl));

        Integer echoString = demoService.hello(10, 10);
        assertThat(echoString, is(20));

        exporter.unexport();
    }

    @Test(expected = RpcException.class)
    public void testServletWithoutWebConfig() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);

        URL servletUrl = exportUrl.addParameter(Constants.SERVER_KEY, "servlet");

        protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, servletUrl));
    }

    @Test(expected = RpcException.class)
    public void testErrorHandler() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);

        URL nettyUrl = exportUrl.addParameter(Constants.SERVER_KEY, "netty");
        Exporter<IDemoService> exporter = protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, nettyUrl));

        IDemoService demoService = this.proxy.getProxy(protocol.refer(IDemoService.class, nettyUrl));

        demoService.error();
    }

    @Test
    public void testInvoke() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);


        Exporter<IDemoService> exporter = protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, exportUrl));

        RpcInvocation rpcInvocation = new RpcInvocation("hello", new Class[]{Integer.class, Integer.class}, new Integer[]{2, 3});

        Result result = exporter.getInvoker().invoke(rpcInvocation);
        assertThat(result.getValue(), CoreMatchers.<Object>is(5));
    }

    @Test
    public void testFilter() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);

        URL nettyUrl = exportUrl.addParameter(Constants.SERVER_KEY, "netty")
                .addParameter(Constants.EXTENSION_KEY, "com.alibaba.dubbo.rpc.protocol.rest.support.LoggingFilter");
        Exporter<IDemoService> exporter = protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, nettyUrl));

        IDemoService demoService = this.proxy.getProxy(protocol.refer(IDemoService.class, nettyUrl));

        Integer result = demoService.hello(1, 2);

        assertThat(result, is(3));

        exporter.unexport();
    }

    @Test(expected = RuntimeException.class)
    public void testRegFail() {
        StaticContext.getContext(Constants.SERVICE_IMPL_CLASS).put(exportUrl.getServiceKey(), DemoService.class);

        URL nettyUrl = exportUrl.addParameter(Constants.EXTENSION_KEY, "com.not.existing.Filter");
        protocol.export(proxy.getInvoker(new DemoService(), IDemoService.class, nettyUrl));
    }

    @Test
    public void testDefaultPort() {
        assertThat(protocol.getDefaultPort(), is(80));
    }
}