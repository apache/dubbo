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

import com.alibaba.dubbo.container.Main;
import com.alibaba.dubbo.container.spring.SpringContainer;
import com.alibaba.dubbo.demo.DemoService;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

    public static void main(String[] args) {

        // Prevent to get IPV6 address,this way only work in debug mode
        // But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Enable shutdown gracefully feature
        System.setProperty(Main.SHUTDOWN_HOOK_KEY, "true");

        // Search provider definition path
        System.setProperty(SpringContainer.SPRING_CONFIG, "META-INF/spring/dubbo-demo-consumer.xml");

        // Refer service
        Main.main(args);
    }

    static class DemoServiceConsumer implements ApplicationContextAware {

        private ApplicationContext context;

        public void init(){

            DemoService demoService = context.getBean(DemoService.class);

            while (true) {
                try {
                    Thread.sleep(1000);
                    // call remote method
                    String hello = demoService.sayHello("world");
                    // get result
                    System.out.println(hello);

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.context = applicationContext;
        }
    }
}
