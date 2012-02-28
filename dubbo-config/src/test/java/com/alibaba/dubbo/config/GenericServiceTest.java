/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.config;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.dubbo.config.api.DemoException;
import com.alibaba.dubbo.config.api.DemoService;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * GenericServiceTest
 * 
 * @author william.liangf
 */
public class GenericServiceTest {
    
    @Test
    public void testGenericServiceException() {
        ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
        service.setApplication(new ApplicationConfig("generic-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29581));
        service.setInterface(DemoService.class.getName());
        service.setRef(new GenericService() {
            public Object $invoke(String method, String[] parameterTypes, Object[] args)
                    throws GenericException {
                if ("sayName".equals(method)) {
                    return "Generic " + args[0];
                }
                if ("throwDemoException".equals(method)) {
                    throw new GenericException(DemoException.class.getName(), "Generic");
                }
                return null;
            }
        });
        service.export();
        try {
            ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
            reference.setApplication(new ApplicationConfig("generic-consumer"));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://127.0.0.1:29581?generic=true");
            DemoService demoService = reference.get();
            try {
                Assert.assertEquals("Generic Haha", demoService.sayName("Haha"));
                try {
                    demoService.throwDemoException();
                    Assert.fail();
                } catch (DemoException e) {
                    Assert.assertEquals("Generic", e.getMessage());
                }
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

}
