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
package com.alibaba.dubbo.examples.validation;

import com.alibaba.dubbo.examples.validation.api.ValidationParameter;
import com.alibaba.dubbo.examples.validation.api.ValidationService;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Date;
import java.util.Set;

/**
 * ValidationTest
 *
 */
public class ValidationTest {

    @Test
    public void testValidation() {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ValidationTest.class.getPackage().getName().replace('.', '/') + "/validation-provider.xml");
        providerContext.start();
        try {
            ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(ValidationTest.class.getPackage().getName().replace('.', '/') + "/validation-consumer.xml");
            consumerContext.start();
            try {
                ValidationService validationService = (ValidationService) consumerContext.getBean("validationService");

                // Save OK
                ValidationParameter parameter = new ValidationParameter();
                parameter.setName("liangfei");
                parameter.setEmail("liangfei@liang.fei");
                parameter.setAge(50);
                parameter.setLoginDate(new Date(System.currentTimeMillis() - 1000000));
                parameter.setExpiryDate(new Date(System.currentTimeMillis() + 1000000));
                validationService.save(parameter);

                try {
                    parameter = new ValidationParameter();
                    parameter.setName("l");
                    parameter.setEmail("liangfei@liang.fei");
                    parameter.setAge(50);
                    parameter.setLoginDate(new Date(System.currentTimeMillis() - 1000000));
                    parameter.setExpiryDate(new Date(System.currentTimeMillis() + 1000000));
                    validationService.save(parameter);
                    Assert.fail();
                } catch (ConstraintViolationException ve) {
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                }

                // Save Error
                try {
                    parameter = new ValidationParameter();
                    validationService.save(parameter);
                    Assert.fail();
                } catch (ConstraintViolationException ve) {
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                }

                // Delete OK
                validationService.delete(2, "abc");

                // Delete Error
                try {
                    validationService.delete(2, "a");
                    Assert.fail();
                } catch (ConstraintViolationException ve) {
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                    Assert.assertEquals(1, violations.size());
                }

                // Delete Error
                try {
                    validationService.delete(0, "abc");
                    Assert.fail();
                } catch (ConstraintViolationException ve) {
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                    Assert.assertEquals(1, violations.size());
                }
                try {
                    validationService.delete(2, null);
                    Assert.fail();
                } catch (ConstraintViolationException ve) {
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                    Assert.assertEquals(1, violations.size());
                }
                try {
                    validationService.delete(0, null);
                    Assert.fail();
                } catch (ConstraintViolationException ve) {
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Assert.assertNotNull(violations);
                    Assert.assertEquals(2, violations.size());
                }
            } finally {
                consumerContext.stop();
                consumerContext.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

}
