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
package com.alibaba.dubbo.config.test;

import java.util.Date;

import org.junit.Test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.proxy.ProxyFactory;
import com.alibaba.dubbo.rpc.util.MyException;
import com.alibaba.dubbo.rpc.util.MyService;
import com.alibaba.dubbo.rpc.util.MyServiceImpl;

/**
 * User: heyman
 * Date: 5/18/11
 * Time: 4:20 PM
 */
@SuppressWarnings({"deprecation","rawtypes","unchecked"})
public class ExceptionTest {
    
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    
    @Test(expected = RuntimeException.class)
    public void testException() {
        URL url = URL.valueOf("dubbo://127.0.0.1:9010/TestService?codec=dubbo");
        protocol.export(proxy.getInvoker(new MyServiceImpl(), MyService.class, url));
        MyService service = proxy.getProxy(protocol.refer(MyService.class, url));
        try {
            service.throwException();
        } catch (MyException e) {
            //success
        }
        service.throwSystemException();
    }

    @Test
    public void testMultiClients() {
        URL url1 = URL.valueOf("dubbo://127.0.0.1:9010/TestService?codec=dubbo&channels=3");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:9010/TestService2?codec=dubbo&channels=3");
        protocol.export(proxy.getInvoker(new MyServiceImpl(), MyService.class, url1));
        protocol.export(proxy.getInvoker(new MyServiceImpl(), MyService.class, url2));
        MyService service = proxy.getProxy(protocol.refer(MyService.class, url1));
        MyService service2 = proxy.getProxy(protocol.refer(MyService.class, url2));
        service.echo("test");
        /*service.echo("test");
        service.echo("test");*/
        service2.echo("test");
    }

    @Test
    public void multiContect() {

        ApplicationConfig application = new ApplicationConfig();
        application.setName("TestServiceServer");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("N/A");
        /*List<RegistryConfig> rcList = new ArrayList<RegistryConfig>();
        rcList.add(registry);*/

        // 服务提供者全局配置
        ProviderConfig provider = new ProviderConfig();
        provider.setPort(9091);
        provider.setThreads(10);
        provider.setRegistry(registry);
        // provider.setCodec("java");
        //provider.setPayload(32 * 1024 * 1024); // 32m max databuffer

        // 服务提供者暴露服务配置
        ServiceConfig<MyService> service = new ServiceConfig<MyService>();
        service.setProvider(provider); // 多个提供者可以用setProviders()
        service.setApplication(application);
        //service.setRegistry(null);
        service.setInterfaceClass(MyService.class);
        service.setRef(new MyServiceImpl());
        service.setVersion("1.0.0");
        service.setPath("testService");
        service.export(); // 注意：此调用必需，将触发服务注册。

        ApplicationConfig applicationc = new ApplicationConfig();
        applicationc.setName("TestServiceServer");
        // 服务消费者全局配置
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setTimeout(5000);
        consumer.setRetries(0);

        // 引用远程服务
        ReferenceConfig reference = new ReferenceConfig();
        //reference.setRegistry(registry); // 多个注册中心可以用setRegistries()
        reference.setApplication(application);
        reference.setConsumer(consumer);
        reference.setInterfaceClass(MyService.class);
        reference.setVersion("1.0.0");
        //reference.setMethods(methods);
        reference.setTimeout(60 * 1000); //超时60秒
        reference.setCheck(false);
        /*Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("codec", "hessian2");
        reference.setParameters(parameters);*/
        reference.setUrl("dubbo://127.0.0.1:9091/testService");
        reference.get();
    }

    @Test
    public void test() {
        System.out.println((new Date()).getTime());
    }

}