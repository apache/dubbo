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
package com.alibaba.dubbo.callback.implicit.test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.registry.support.SimpleRegistryExporter;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * @author chao.liuc
 */
public class CallbackTest {

    @Before
    public void setUp() throws Exception {
        SimpleRegistryExporter.exportIfAbsent(9234);
    }
    
    public static void main(String[] args) throws InterruptedException {
        startProvider();
        synchronized (CallbackTest.class) {
            CallbackTest.class.wait();
        }
    }
    
    public static AbstractApplicationContext startProvider() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:com/alibaba/dubbo/callback/implicit/provider.xml");
        context.start();
        return context ;
    }
    
    @Test
    public void TestCallback_Close() throws Exception {
        AbstractApplicationContext providerContext = startProvider();
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:com/alibaba/dubbo/callback/implicit/consumer2.xml");
        context.start();

        IDemoService demoService = (IDemoService) context.getBean("demoService");
        int requestId = 2;
        Person ret = demoService.get(requestId);
        Assert.assertEquals(requestId, ret.getId());
        context.destroy();
        providerContext.destroy();
    }

    @Test
    public void TestCallback_Open() throws Exception {
        AbstractApplicationContext providerContext = startProvider();
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:com/alibaba/dubbo/callback/implicit/consumer.xml");
        context.start();

        IDemoService demoService = (IDemoService) context.getBean("demoService");
        NofifyImpl notify = (NofifyImpl) context.getBean("demoCallback");

        int requestId = 2;
        Person ret = demoService.get(requestId);
        Assert.assertEquals(null, ret);
        for (int i = 0; i < 10; i++) {
            if (!notify.ret.containsKey(requestId)) {
                Thread.sleep(200);
            } else {
                break;
            }
        }
        Assert.assertEquals(requestId, notify.ret.get(requestId).getId());
        
        context.destroy();
        providerContext.destroy();
    }
    @Test
    public void Test_Async_Call() throws Exception {
        AbstractApplicationContext providerContext = startProvider();
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:com/alibaba/dubbo/callback/implicit/consumer_async.xml");
        context.start();

        IDemoService demoService = (IDemoService) context.getBean("demoService");


        int requestId1 = 1;
        Person ret = demoService.get(requestId1);
        Assert.assertEquals(null, ret);
        Future<Person> p1Future = RpcContext.getContext().getFuture();
         
        int requestId2 = 2;
        Person ret2 = demoService.get(requestId2);
        Assert.assertEquals(null, ret2);
        Future<Person> p2Future = RpcContext.getContext().getFuture();
         
        ret = p1Future.get(1000, TimeUnit.MICROSECONDS);
        ret2 = p2Future.get(1000, TimeUnit.MICROSECONDS);
        Assert.assertEquals(requestId1, ret.getId());
        Assert.assertEquals(requestId2, ret2.getId());
        
        context.destroy();
        providerContext.destroy();
    }
}