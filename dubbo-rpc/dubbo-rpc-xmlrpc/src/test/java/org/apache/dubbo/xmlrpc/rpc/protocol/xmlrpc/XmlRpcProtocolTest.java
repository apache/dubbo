package io.github.kimmking.dubbo.rpc.protocol.xmlrpc;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;

/**
 * Created by kimmking(kimmking@163.com) on 2018/3/28.
 */
public class XmlRpcProtocolTest {

    @Test
    public void testXmlRpcProtocol() {
        XmlRpcServiceImpl server = new XmlRpcServiceImpl();
        Assert.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("xmlrpc://127.0.0.1:5342/" + XmlRpcService.class.getName() + "?version=1.0.0");
        Exporter<XmlRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, XmlRpcService.class, url));
        Invoker<XmlRpcService> invoker = protocol.refer(XmlRpcService.class, url);
        XmlRpcService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assert.assertTrue(server.isCalled());
        Assert.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testXmlRpcProtocolForServerJetty9() {
        XmlRpcServiceImpl server = new XmlRpcServiceImpl();
        Assert.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("xmlrpc://127.0.0.1:5342/" + XmlRpcService.class.getName() + "?version=1.0.0&server=jetty9");
        Exporter<XmlRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, XmlRpcService.class, url));
        Invoker<XmlRpcService> invoker = protocol.refer(XmlRpcService.class, url);
        XmlRpcService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assert.assertTrue(server.isCalled());
        Assert.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    @Ignore
    public void testCustomException() {
        XmlRpcServiceImpl server = new XmlRpcServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("xmlrpc://127.0.0.1:5342/" +
                XmlRpcService.class.getName() + "?version=1.0.0&server=jetty9");
        Exporter<XmlRpcService> exporter = protocol.export(proxyFactory.getInvoker(server, XmlRpcService.class, url));
        Invoker<XmlRpcService> invoker = protocol.refer(XmlRpcService.class, url);
        XmlRpcService client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            Assert.fail();
        } catch (XmlRpcServiceImpl.MyException expected) {
        }
        invoker.destroy();
        exporter.unexport();
    }

}