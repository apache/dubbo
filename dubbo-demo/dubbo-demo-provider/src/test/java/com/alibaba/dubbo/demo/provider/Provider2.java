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
package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.concurrent.Executors;

public class Provider2 {

    ClassPathXmlApplicationContext context;


    @Before
    public void before() {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider2.xml"});
        context.start();
    }
    @Test
    public void main() throws IOException {
        System.in.read(); // press any key to exit
    }

    /**
     * 测试本地暴露
     * @throws Exception
     */
    @Test
    public void test_InJvm() throws Exception {
        final DemoService demoService = (DemoService) context.getBean("demoService2");
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                demoService.sayHello("a");
            }
        });
    }

    public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider1.xml"});
        context.start();
        try {
            System.in.read(); // press any key to exit
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
