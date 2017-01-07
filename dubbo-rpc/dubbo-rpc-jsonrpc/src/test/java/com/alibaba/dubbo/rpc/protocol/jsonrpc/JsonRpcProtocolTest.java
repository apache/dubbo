package com.alibaba.dubbo.rpc.protocol.jsonrpc;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.jsonrpc.JsonRpcServiceImpl.MyException;

/**
 * JsonRpcProtocolTest Created by wuwen on 15/4/1.
 */
public class JsonRpcProtocolTest {

    @Test
    public void testJsonrpcProtocol() {
        JsonRpcServiceImpl server = new JsonRpcServiceImpl();
        Assert.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("jsonrpc://127.0.0.1:5342/" + JsonRpcService.class.getName() + "?version=1.0.0");
        Exporter<JsonRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, JsonRpcService.class, url));
        Invoker<JsonRpcService> invoker = protocol.refer(JsonRpcService.class, url);
        JsonRpcService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assert.assertTrue(server.isCalled());
        Assert.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testJsonrpcProtocolForServerJetty9() {
        JsonRpcServiceImpl server = new JsonRpcServiceImpl();
        Assert.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("jsonrpc://127.0.0.1:5342/" + JsonRpcService.class.getName() + "?version=1.0.0&server=jetty9");
        Exporter<JsonRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, JsonRpcService.class, url));
        Invoker<JsonRpcService> invoker = protocol.refer(JsonRpcService.class, url);
        JsonRpcService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assert.assertTrue(server.isCalled());
        Assert.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    // TODO 暂不支持自定义异常

    @Test
    @Ignore
    public void testCustomException() {
        JsonRpcServiceImpl server = new JsonRpcServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("jsonrpc://127.0.0.1:5342/" +
                JsonRpcService.class.getName() + "?version=1.0.0&server=jetty9");
        Exporter<JsonRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, JsonRpcService.class, url));
        Invoker<JsonRpcService> invoker = protocol.refer(JsonRpcService.class, url);
        JsonRpcService client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            Assert.fail();
        } catch (MyException expected) {
        }
        invoker.destroy();
        exporter.unexport();
    }

}