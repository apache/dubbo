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
package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.demo.api.DemoService;
import com.alibaba.dubbo.demo.provider.impl.DemoServiceImpl;

/**
 * CodeProvider
 * 
 * @author william.liangf
 */
public class CodeProvider {
    
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        DemoService demoService = new DemoServiceImpl();
        
        ApplicationConfig application = new ApplicationConfig();
        application.setName("code-provider");
        
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("10.20.153.28:9090");
        registry.setUsername("admin");
        registry.setPassword("hello1234");
        
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(20883);
        
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.setInterfaceClass(DemoService.class);
        service.setVersion("1.0.0");
        service.setRef(demoService);
        service.export();
        
        System.in.read(); // 输入任意键退出
    }

}