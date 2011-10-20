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

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.api.Address;
import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.api.Role;
import com.alibaba.dubbo.demo.api.User;
import com.alibaba.dubbo.demo.api.Welcome;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * CodeProvider
 * 
 * @author william.liangf
 */
public class CodeConsumer {
    
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("code-consumer");
        
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("10.20.153.28:9090");
        registry.setUsername("admin");
        registry.setPassword("hello1234");
        
        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterfaceClass(DemoService.class);
        reference.setVersion("1.0.0");
        DemoService demoService = reference.get();
        
        User user = new User("liangfei", 25, new String[]{"13012345678", "1308654321"}, new Address("hangzhou", "wangshang", "310052"), Role.MEMBER);
        for (int i = 0; i < 1; i ++) {
            try {
                RpcContext.getContext().setAttachment("i", String.valueOf(i));
                Welcome result = demoService.sayHello(user);
                System.out.println("-------- (" + i + ") " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        reference.destroy();
        
        synchronized (CodeConsumer.class) {
            while (true) {
                try {
                    CodeConsumer.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}