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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.DubboAppender;
import com.alibaba.dubbo.common.utils.LogUtil;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;

public class ReferenceCountExchangeClientTest {
    
    Exporter<?> demoExporter ;
    Exporter<?> helloExporter ;
    
    Invoker<IDemoService> demoServiceInvoker;
    Invoker<IHelloService> helloServiceInvoker;
    
    IDemoService demoService ;
    IHelloService helloService;
    
    ExchangeClient demoClient ;
    ExchangeClient helloClient ;
    
    String errorMsg = "safe guard client , should not be called ,must have a bug";
    
    public interface IDemoService{
        public String demo();
    }
    public class DemoServiceImpl implements IDemoService{
        public String demo(){
            return "demo";
        }
    }
    public interface IHelloService{
        public String hello();
    }
    public class HelloServiceImpl implements IHelloService{
        public String hello(){
            return "hello";
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    /**
     * 测试共享连接
     */
    @Test
    public void test_share_connect(){
        init(0);
        Assert.assertEquals(demoClient.getLocalAddress(), helloClient.getLocalAddress());
        Assert.assertEquals(demoClient, helloClient);
        destoy();
    }
    
    /**
     * 测试不共享连接
     */
    @Test
    public void test_not_share_connect(){
        init(1);
        Assert.assertNotSame(demoClient.getLocalAddress(), helloClient.getLocalAddress());
        Assert.assertNotSame(demoClient, helloClient);
        destoy();
    }
    
    /**
     * 测试invoker多次destory不会导致计数器多次减少
     */
    @Test
    public void test_multi_destory(){
        init(0);
        DubboAppender.doStart();
        DubboAppender.clear();
        demoServiceInvoker.destroy();
        demoServiceInvoker.destroy();
        Assert.assertEquals("hello", helloService.hello());
        Assert.assertEquals("should not  warning message", 0, LogUtil.findMessage(errorMsg));
        LogUtil.checkNoError();
        DubboAppender.doStop();
        destoy();
    }
    
    /**
     * 测试计数器错误，调用成功
     */
    @Test
    public void test_counter_error(){
        init(0);
        DubboAppender.doStart();
        DubboAppender.clear();
        
        ReferenceCountExchangeClient client = getReferenceClient(helloServiceInvoker);
        //close一次，计数器从2减少到1，不能warning
        client.close();
        Assert.assertEquals("hello", helloService.hello());
        Assert.assertEquals("should not warning message", 0, LogUtil.findMessage(errorMsg));
        //计数器错误，调用正常
        client.close();
        Assert.assertEquals("hello", helloService.hello());
        Assert.assertEquals("should warning message", 1, LogUtil.findMessage(errorMsg));
       
        //调用5千次输出一个错误
        Assert.assertEquals("hello", helloService.hello());
        Assert.assertEquals("should warning message", 1, LogUtil.findMessage(errorMsg));
        
        DubboAppender.doStop();
        
        //重新调用一次后status已经是available.
        Assert.assertEquals("client status available", true, helloServiceInvoker.isAvailable());
        
        client.close();
        //client已经被替换为lazyclient lazy client从referenceclientmap中获取，获取到的是上次的client（已经被调用过一次），所以close状态为false
        Assert.assertEquals("client status close", false, client.isClosed());
        Assert.assertEquals("client status close", false, helloServiceInvoker.isAvailable());
        destoy();
    }
    
    @SuppressWarnings("unchecked")
    private void init(int connections){
        int port = NetUtils.getAvailablePort();
        URL demoUrl = URL.valueOf("dubbo://127.0.0.1:"+port+"/demo?"+Constants.CONNECTIONS_KEY+"="+connections);
        URL helloUrl = URL.valueOf("dubbo://127.0.0.1:"+port+"/hello?"+Constants.CONNECTIONS_KEY+"="+connections);
        
        demoExporter = export(new DemoServiceImpl(), IDemoService.class, demoUrl);
        helloExporter = export(new HelloServiceImpl(), IHelloService.class, helloUrl);
        
        demoServiceInvoker = (Invoker<IDemoService>) referInvoker(IDemoService.class, demoUrl);
        demoService = proxy.getProxy(demoServiceInvoker);
        Assert.assertEquals("demo", demoService.demo());
        
        helloServiceInvoker = (Invoker<IHelloService>) referInvoker(IHelloService.class, helloUrl);
        helloService = proxy.getProxy(helloServiceInvoker);
        Assert.assertEquals("hello", helloService.hello());
        
        demoClient = getClient(demoServiceInvoker);
        helloClient = getClient(helloServiceInvoker);
    }
    
    private void destoy(){
        demoServiceInvoker.destroy();
        helloServiceInvoker.destroy();
        demoExporter.getInvoker().destroy();
        helloExporter.getInvoker().destroy();
    }
    
    private ExchangeClient getClient(Invoker<?> invoker){
        if (invoker.getUrl().getParameter(Constants.CONNECTIONS_KEY, 1) == 1){
            return getInvokerClient(invoker);
        } else {
            ReferenceCountExchangeClient client = getReferenceClient(invoker);
            try {
                Field clientField = ReferenceCountExchangeClient.class.getDeclaredField("client");
                clientField.setAccessible(true);
                return (ExchangeClient) clientField.get(client);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
    
    private ReferenceCountExchangeClient getReferenceClient(Invoker<?> invoker){
        return (ReferenceCountExchangeClient)getInvokerClient(invoker);
    }
    
    private ExchangeClient getInvokerClient(Invoker<?> invoker){
        @SuppressWarnings("rawtypes")
        DubboInvoker dInvoker = (DubboInvoker)invoker;
        try {
            Field clientField = DubboInvoker.class.getDeclaredField("clients");
            clientField.setAccessible(true);
            ExchangeClient[] clients = (ExchangeClient[]) clientField.get(dInvoker);
            return clients[0];
            
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private static DubboProtocol     protocol = DubboProtocol.getDubboProtocol();
    public static  ProxyFactory proxy    = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    public static Invoker<?> referInvoker(Class<?> type, URL url) {
        return (Invoker<?>)protocol.refer(type, url);
    }

    public static <T> Exporter<T> export(T instance, Class<T> type, String url) {
        return export(instance, type, URL.valueOf(url));
    }
    
    public static <T> Exporter<T> export(T instance, Class<T> type, URL url) {
        return protocol.export(proxy.getInvoker(instance, type, url));
    }
}