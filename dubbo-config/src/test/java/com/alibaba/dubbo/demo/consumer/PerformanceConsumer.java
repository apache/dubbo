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
package com.alibaba.dubbo.demo.consumer;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.demo.api.Address;
import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.api.Role;
import com.alibaba.dubbo.demo.api.User;
import com.alibaba.dubbo.demo.api.Welcome;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * SimpleConsumer
 * 
 * @author william.liangf
 */
public class PerformanceConsumer {

    public static void main(String[] args) throws Throwable {
        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:performance_consumer.xml");      
        context.start();
        final DemoService demoService = (DemoService)context.getBean("demoService");
    	final User user = new User("liangfei", 25, new String[]{"13012345678", "1308654321"}, new Address("hangzhou", "wangshang", "310052"), Role.MEMBER);
        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger error = new AtomicInteger();
    	for (int n = 0; n < 11; n ++) {
            new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < Integer.MAX_VALUE; i ++) {
                        try {
                            int c = count.incrementAndGet();
                            if (c % 100 == 0) {
                                System.out.println("count: " + count.get() + ", error: " + error.get());
                            }
                            RpcContext.getContext().setAttachment("i", String.valueOf(i));
                            Welcome result = demoService.sayHello(user);
                            // System.out.println(result);
                            User user = result.getUser();
                            if (! "liangfei".equals(user.getName())) {
                                throw new IllegalStateException("Invalid result " + user.getName());
                            }
                        } catch (Exception e) {
                            error.incrementAndGet();
                            //if (e.getMessage().contains("ExecutionException")) {
                                e.printStackTrace();
                            //}
                            System.out.println("count: " + count.get() + ", error: " + error.get());
                        }
                    }
                }
            }).start();
        }
    }

}