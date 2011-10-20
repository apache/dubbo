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

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.demo.api.Address;
import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.api.Role;
import com.alibaba.dubbo.demo.api.User;
import com.alibaba.dubbo.demo.api.Welcome;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * DirectConsumer
 * 
 * @author william.liangf
 */
public class DirectConsumer {

    public static void main(String[] args) throws Throwable {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:direct_consumer.xml");      
        context.start();
        DemoService demoService = (DemoService)context.getBean("demoService");
        User user = new User("liangfei", 25, new String[]{"13012345678", "1308654321"}, new Address("hangzhou", "wangshang", "310052"), Role.MEMBER);
        for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            Thread.sleep(2000);
            try {
                RpcContext.getContext().setAttachment("i", String.valueOf(i));
                Welcome result = demoService.sayHello(user);
                System.out.println("-------- (" + i + ") " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}