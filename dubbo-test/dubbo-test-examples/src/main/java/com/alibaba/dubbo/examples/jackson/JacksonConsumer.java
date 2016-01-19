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
package com.alibaba.dubbo.examples.jackson;

import com.alibaba.dubbo.examples.jackson.api.*;
import com.google.common.collect.Lists;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;
import java.util.List;

/**
 * JacksonConsumer
 *
 * @author dylan
 */
public class JacksonConsumer {

    public static void main(String[] args) throws Exception {
        String config = JacksonConsumer.class.getPackage().getName().replace('.', '/') + "/jackson-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        JacksonService jacksonService = (JacksonService) context.getBean("jacksonService");
        System.out.println("TEST: sayHello");
        String hello = jacksonService.sayHello("world");
        System.out.println(hello);

        System.out.println("TEST: testJacksonBean");
        JacksonBean jacksonBean = jacksonService.testJacksonBean(new JacksonBean(), new JacksonInnerBean());
        System.out.println(jacksonBean);

        System.out.println("TEST: testInheritBean");
        Inherit inherit = jacksonService.testInheritBean(new InheritBean(), new JacksonBean());
        System.out.println(inherit);

        System.out.println("TEST: testArray");
        int[] intArray = jacksonService.testArray(new int[]{1,2});
        System.out.println(Arrays.toString(intArray));

        System.out.println("TEST: testBeanArray");
        JacksonBean[] beanArray = jacksonService.testBeanArray(new JacksonBean[]{new JacksonBean(), new JacksonBean()});
        System.out.println(Arrays.toString(beanArray));

        System.out.println("TEST: testException");
        try {
            jacksonService.testException();
        } catch(Exception e){
            System.out.println("exception : " + e.getClass() + " : " + e.getMessage());
        }

        System.in.read();
    }

}
