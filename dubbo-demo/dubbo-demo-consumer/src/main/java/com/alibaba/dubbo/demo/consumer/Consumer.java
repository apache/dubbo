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
package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

    public static void main(String[] args) {
        test1();
        //return;

        // 当前应用配置
//        ApplicationConfig application = new ApplicationConfig();
//        application.setName("yyy");
//
//// 连接注册中心配置
//        RegistryConfig registry = new RegistryConfig();
//        registry.setAddress("multicast://224.5.6.7:1234");
//        //registry.setUsername("aaa");
//        //registry.setPassword("bbb");
//
//// 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接
//
//// 引用远程服务
//        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>(); // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
//        reference.setApplication(application);
//        reference.setRegistry(registry); // 多个注册中心可以用setRegistries()
//        reference.setInterface(DemoService.class);
//        reference.setVersion("1.0.0");
//
//// 和本地bean一样使用xxxService
//        DemoService xxxService = reference.get();

    }

    /**
     * 使用了xml进行调用
     */
    private static void test1() {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy


        while (true) {
            //    try {
            //        //基本对象处理
            //        Thread.sleep(1000);
            //        String hello = demoService.sayHello("world"); // call remote method
            //        System.out.println(hello); // get result
            //
            //        //对象处理
            //        Thread.sleep(1000);
            //        System.out.println("**********");
            //        SimpleObject simpleObject = new SimpleObject();
            //        simpleObject.setNum(1);
            //        System.out.println(demoService.changeSimple(simpleObject));
            //        demoService.throwsEx();
            //
            //    } catch (Throwable throwable) {
            //        throwable.printStackTrace();
            //    }
            System.out.println(demoService.sayHello("11"));
        }
    }

}
