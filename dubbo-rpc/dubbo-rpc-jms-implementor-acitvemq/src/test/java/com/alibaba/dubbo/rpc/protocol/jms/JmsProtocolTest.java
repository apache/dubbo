/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.jms;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.jms.JmsServiceImpl.MyException;

/**
 * @author Kimmking
 */
public class JmsProtocolTest {
    
    @Test
    public void testJmsProtocol() {
        JmsServiceImpl server = new JmsServiceImpl();
        Assert.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("jms://127.0.0.1:5342/" + JmsService.class.getName() + "?version=1.0.0");
        Exporter<JmsService> exporter = protocol.export(proxyFactory.getInvoker(server, JmsService.class, url));
        Invoker<JmsService> invoker = protocol.refer(JmsService.class, url);
        JmsService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assert.assertTrue(server.isCalled());
        Assert.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testTimeOut() {
        JmsServiceImpl server = new JmsServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("jms://127.0.0.1:5343/" + JmsService.class.getName() + "?version=1.0.0&timeout=100");
        Exporter<JmsService> exporter = protocol.export(proxyFactory.getInvoker(server, JmsService.class, url));
        Invoker<JmsService> invoker = protocol.refer(JmsService.class, url);
        JmsService client = proxyFactory.getProxy(invoker);
        try {
            client.timeOut(600);
            Assert.fail();
        } catch (RpcException expected) {
            Assert.assertEquals(true, expected.isTimeout());
            System.out.println(expected.getMessage());
        }finally{
            invoker.destroy();
            exporter.unexport();
        }
        
    }
    
    @Test
    public void testCustomException() {
        JmsServiceImpl server = new JmsServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("jms://127.0.0.1:5344/" + JmsService.class.getName() + "?version=1.0.0");
        Exporter<JmsService> exporter = protocol.export(proxyFactory.getInvoker(server, JmsService.class, url));
        Invoker<JmsService> invoker = protocol.refer(JmsService.class, url);
        JmsService client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            Assert.fail();
        } catch (MyException expected) {
        }
        invoker.destroy();
        exporter.unexport();
    }

}