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
package org.apache.dubbo.rpc.protocol.dubbo.support;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EnumBak {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testNormal() {
        int port = NetUtils.getAvailablePort();
        URL serviceurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?proxy=jdk"
                + "&interface=" + DemoService.class.getName()
                + "&timeout=" + Integer.MAX_VALUE
        );
        DemoService demo = new DemoServiceImpl();
        Invoker<DemoService> invoker = proxy.getInvoker(demo, DemoService.class, serviceurl);
        protocol.export(invoker);

        URL consumerurl = serviceurl;
        Invoker<DemoService> reference = protocol.refer(DemoService.class, consumerurl);
        DemoService demoProxy = (DemoService) proxy.getProxy(reference);
//        System.out.println(demoProxy.getThreadName());
        Assert.assertEquals((byte) -128, demoProxy.getbyte((byte) -128));

//        invoker.destroy();
        reference.destroy();
    }

    @Ignore
    @Test
    public void testExportService() throws InterruptedException {
        int port = NetUtils.getAvailablePort();
        URL serviceurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?proxy=jdk&timeout=" + Integer.MAX_VALUE
        );
        DemoService demo = new DemoServiceImpl();
        Invoker<DemoService> invoker = proxy.getInvoker(demo, DemoService.class, serviceurl);
        protocol.export(invoker);
        synchronized (EnumBak.class) {
            EnumBak.class.wait();
        }

//        URL consumerurl = serviceurl;
//        Invoker<DemoService> reference = protocol.refer(DemoService.class, consumerurl);
//        DemoService demoProxy = (DemoService)proxyFactory.createProxy(reference);
////        System.out.println(demoProxy.getThreadName());
//        System.out.println("byte:"+demoProxy.getbyte((byte)-128));
//
//        invoker.destroy();
//        reference.destroy();
    }

    @Test
    public void testNormalEnum() {
        int port = NetUtils.getAvailablePort();
        URL serviceurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?timeout=" + Integer.MAX_VALUE
        );
        DemoService demo = new DemoServiceImpl();
        Invoker<DemoService> invoker = proxy.getInvoker(demo, DemoService.class, serviceurl);
        protocol.export(invoker);

        URL consumerurl = serviceurl;
        Invoker<DemoService> reference = protocol.refer(DemoService.class, consumerurl);
        DemoService demoProxy = (DemoService) proxy.getProxy(reference);
        Type type = demoProxy.enumlength(Type.High);
        System.out.println(type);
        Assert.assertEquals(Type.High, type);

        invoker.destroy();
        reference.destroy();
    }

    // verify compatibility when 2.0.5 invokes 2.0.3
    @Ignore
    @Test
    public void testEnumCompat() {
        int port = 20880;
        URL consumerurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?timeout=" + Integer.MAX_VALUE
        );
        Invoker<DemoService> reference = protocol.refer(DemoService.class, consumerurl);
        DemoService demoProxy = (DemoService) proxy.getProxy(reference);
        Type type = demoProxy.enumlength(Type.High);
        System.out.println(type);
        Assert.assertEquals(Type.High, type);
        reference.destroy();
    }

    // verify compatibility when 2.0.5 invokes 2.0.3
    @Ignore
    @Test
    public void testGenricEnumCompat() {
        int port = 20880;
        URL consumerurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?timeout=" + Integer.MAX_VALUE
        );
        Invoker<GenericService> reference = protocol.refer(GenericService.class, consumerurl);

        GenericService demoProxy = (GenericService) proxy.getProxy(reference);
        Object obj = demoProxy.$invoke("enumlength", new String[]{Type[].class.getName()}, new Object[]{new Type[]{Type.High, Type.High}});
        System.out.println("obj---------->" + obj);
        reference.destroy();
    }

    // verify compatibility when 2.0.5 invokes 2.0.3, enum in custom parameter
    @Ignore
    @Test
    public void testGenricCustomArg() {

        int port = 20880;
        URL consumerurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?timeout=2000000"
        );
        Invoker<GenericService> reference = protocol.refer(GenericService.class, consumerurl);

        GenericService demoProxy = (GenericService) proxy.getProxy(reference);
        Map<String, Object> arg = new HashMap<String, Object>();
        arg.put("type", "High");
        arg.put("name", "hi");

        Object obj = demoProxy.$invoke("get", new String[]{"org.apache.dubbo.rpc.CustomArgument"}, new Object[]{arg});
        System.out.println("obj---------->" + obj);
        reference.destroy();
    }

    @Ignore
    @Test
    public void testGenericExport() throws InterruptedException {
        int port = NetUtils.getAvailablePort();
        port = 20880;
        URL serviceurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?timeout=" + Integer.MAX_VALUE
        );
        DemoService demo = new DemoServiceImpl();
        Invoker<DemoService> invoker = proxy.getInvoker(demo, DemoService.class, serviceurl);
        protocol.export(invoker);


        //SERVER
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void testGenericEnum() throws InterruptedException {
        int port = NetUtils.getAvailablePort();
        URL serviceurl = URL.valueOf("dubbo://127.0.0.1:" + port + "/test?timeout=" + Integer.MAX_VALUE
        );
        DemoService demo = new DemoServiceImpl();
        Invoker<DemoService> invoker = proxy.getInvoker(demo, DemoService.class, serviceurl);
        protocol.export(invoker);

        URL consumerurl = serviceurl;

        Invoker<GenericService> reference = protocol.refer(GenericService.class, consumerurl);

        GenericService demoProxy = (GenericService) proxy.getProxy(reference);
        Object obj = demoProxy.$invoke("enumlength", new String[]{Type[].class.getName()}, new Object[]{new Type[]{Type.High, Type.High}});
        System.out.println("obj---------->" + obj);

        invoker.destroy();
        reference.destroy();
    }
}
