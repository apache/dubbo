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
package com.alibaba.dubbo.config.validation;

import java.util.Date;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * GenericServiceTest
 * 
 * @author william.liangf
 */
public class ValidationTest {

    @Test
    public void testValidation() {
        ServiceConfig<ValidationService> service = new ServiceConfig<ValidationService>();
        service.setApplication(new ApplicationConfig("validation-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setProtocol(new ProtocolConfig("dubbo", 29582));
        service.setInterface(ValidationService.class.getName());
        service.setRef(new ValidationServiceImpl());
        service.export();
        try {
            ReferenceConfig<ValidationService> reference = new ReferenceConfig<ValidationService>();
            reference.setApplication(new ApplicationConfig("validation-consumer"));
            reference.setInterface(ValidationService.class);
            reference.setUrl("dubbo://127.0.0.1:29582?validation=true");
            ValidationService validationService = reference.get();
            try {
                // Save OK
                ValidationParameter parameter = new ValidationParameter();
                parameter.setName("liangfei");
                parameter.setEmail("liangfei@liang.fei");
                parameter.setAge(50);
                parameter.setLoginDate(new Date(System.currentTimeMillis() - 1000000));
                parameter.setExpiryDate(new Date(System.currentTimeMillis() + 1000000));
                validationService.save(parameter);
                
                // Save Error
                try {
                    parameter = new ValidationParameter();
                    validationService.save(parameter);
                    Assert.fail();
                } catch (RpcException e) {
                    e.printStackTrace();
                    ConstraintViolationException ve = (ConstraintViolationException)e.getCause();
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                }
                
                // Delete OK
                validationService.delete(2);
                
                // Delete Error
                try {
                    validationService.delete(0);
                    Assert.fail();
                } catch (RpcException e) {
                    ConstraintViolationException ve = (ConstraintViolationException)e.getCause();
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                }
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

}
