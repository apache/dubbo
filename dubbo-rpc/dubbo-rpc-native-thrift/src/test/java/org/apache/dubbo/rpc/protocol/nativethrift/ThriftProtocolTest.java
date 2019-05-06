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
package org.apache.dubbo.rpc.protocol.nativethrift;

import com.alibaba.dubbo.rpc.service.GenericService;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.serialize.nativejava.NativeJavaSerialization;
import org.apache.dubbo.rpc.*;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * ThriftProtocolTest
 */
public class ThriftProtocolTest {

    @Test
    public void testThriftProtocol() throws TException{
        DemoServiceImpl server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5341/" + DemoService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<DemoService.Iface> invoker = protocol.refer(DemoService.Iface.class, url);
        DemoService.Iface client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testThriftProtocolMultipleServices() throws TException{

        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        DemoServiceImpl server1 = new DemoServiceImpl();
        URL url1 = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true");
        Exporter<DemoService.Iface> exporter1 = protocol.export(proxyFactory.getInvoker(server1, DemoService.Iface.class, url1));
        Invoker<DemoService.Iface> invoker1 = protocol.refer(DemoService.Iface.class, url1);
        DemoService.Iface client1 = proxyFactory.getProxy(invoker1);
        String result1 = client1.sayHello("haha");
        Assertions.assertTrue(server1.isCalled());
        Assertions.assertEquals("Hello, haha", result1);

        UserServiceImpl server2 = new UserServiceImpl();
        URL url2 = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + UserService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true");
        Exporter<UserService.Iface> exporter2 = protocol.export(proxyFactory.getInvoker(server2, UserService.Iface.class, url2));
        Invoker<UserService.Iface> invoker2 = protocol.refer(UserService.Iface.class, url2);
        UserService.Iface client2 = proxyFactory.getProxy(invoker2);
        String result2 = client2.find(2);
        Assertions.assertEquals("KK2", result2);

        invoker1.destroy();
        exporter1.unexport();
        invoker2.destroy();
        exporter2.unexport();
    }

    @Disabled
    @Test
    public void testGenericInvoke() throws TException{
        DemoServiceImpl server = new DemoServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker);
        String result = (String) client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Disabled
    @Test
    public void testGenericInvokeWithNativeJava() throws IOException, ClassNotFoundException {
        DemoServiceImpl server = new DemoServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0&generic=nativejava");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker);

        Serialization serialization = new NativeJavaSerialization();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeObject("haha");
        objectOutput.flushBuffer();

        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{byteArrayOutputStream.toByteArray()});
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) result);
        ObjectInput objectInput = serialization.deserialize(url, byteArrayInputStream);
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", objectInput.readObject());
        invoker.destroy();
        exporter.unexport();
    }

    @Disabled
    @Test
    public void testGenericInvokeWithRpcContext() throws TException{
        RpcContext.getContext().setAttachment("myContext", "123");

        DemoServiceImpl server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker);
        String result = (String) client.$invoke("context", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertEquals("Hello, haha context, 123", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Disabled
    @Test
    public void testGenericInvokeWithBean() {
        DemoServiceImpl server = new DemoServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0&generic=bean");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker);

        JavaBeanDescriptor javaBeanDescriptor = JavaBeanSerializeUtil.serialize("haha");

        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{javaBeanDescriptor});
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) result));
        invoker.destroy();
        exporter.unexport();
    }

    @Disabled
    @Test
    public void testOverload() throws TException{
        DemoServiceImpl server = new DemoServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0&nthrift.overload.method=true&nthrift2.request=false");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<DemoService.Iface> invoker = protocol.refer(DemoService.Iface.class, url);
        DemoService.Iface client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertEquals("Hello, haha", result);
        result = client.sayHelloTimes("haha", 1);
        Assertions.assertEquals("Hello, haha. ", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Disabled
    @Test
    public void testTimeOut() throws TException{
        DemoServiceImpl server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0&timeout=10");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<DemoService.Iface> invoker = protocol.refer(DemoService.Iface.class, url);
        DemoService.Iface client = proxyFactory.getProxy(invoker);
        try {
            client.timeOut(6000);
            fail();
        } catch (RpcException expected) {
            Assertions.assertTrue(expected.isTimeout());
        } finally {
            invoker.destroy();
            exporter.unexport();
        }

    }

    @Disabled
    @Test
    public void testCustomException() throws TException{
        DemoServiceImpl server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:5342/" + DemoService.Iface.class.getName() + "?version=1.0.0");
        Exporter<DemoService.Iface> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.Iface.class, url));
        Invoker<DemoService.Iface> invoker = protocol.refer(DemoService.Iface.class, url);
        DemoService.Iface client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            fail();
        } catch (DemoServiceImpl.MyException expected) {

        }
        invoker.destroy();
        exporter.unexport();
    }

}
