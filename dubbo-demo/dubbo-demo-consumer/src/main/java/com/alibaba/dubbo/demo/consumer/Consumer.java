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
import com.alibaba.dubbo.demo.RequestParam;
import com.alibaba.dubbo.rpc.RpcException;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Iterator;
import java.util.Set;

public class Consumer {

    public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                String hello = demoService.sayHello("world"); // call remote method
                System.out.println(hello); // get result
                String helloWithParameterValidation = demoService.sayHelloWithParameterValidation(new RequestParam("hello")); // call remote method
                System.out.println(helloWithParameterValidation); // get result
            } catch (RpcException e) {
                if (e.getCause() instanceof ConstraintViolationException) {
                    ConstraintViolationException ve = (ConstraintViolationException) e.getCause();
                    Set<ConstraintViolation<?>> violations = ve.getConstraintViolations();
                    Iterator it = violations.iterator();
                    while (it.hasNext()) {
                        ConstraintViolation cv = (ConstraintViolation) it.next();
                        String name = ((PathImpl) cv.getPropertyPath()).getLeafNode().getName();
                        String message = cv.getMessage();
                        System.out.printf("The value [hello] of the field [%s] check failed with message [%s]", name, message);
                    }
                } else {
                    throw e;
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }

}
