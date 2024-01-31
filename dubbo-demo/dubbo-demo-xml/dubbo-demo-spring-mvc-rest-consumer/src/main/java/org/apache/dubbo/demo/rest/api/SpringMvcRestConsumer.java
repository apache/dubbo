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
package org.apache.dubbo.demo.rest.api;

import java.util.Arrays;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import po.User;

public class SpringMvcRestConsumer {

    public static void main(String[] args) {
        consumerService();
    }

    public static void consumerService() {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(new String[] {"spring/rest-consumer.xml"});
        context.start();
        System.out.println("spring mvc rest consumer start");
        springMvcRestDemoServiceTest(context);
        System.out.println("spring mvc rest consumer test success");
    }

    private static void springMvcRestDemoServiceTest(ClassPathXmlApplicationContext context) {
        SpringRestDemoService springRestDemoService =
                context.getBean("springRestDemoService", SpringRestDemoService.class);
        String hello = springRestDemoService.sayHello("hello");
        assertEquals("Hello, hello", hello);
        Integer result = springRestDemoService.primitiveInt(1, 2);
        Long resultLong = springRestDemoService.primitiveLong(1, 2l);
        long resultByte = springRestDemoService.primitiveByte((byte) 1, 2l);
        long resultShort = springRestDemoService.primitiveShort((short) 1, 2l, 1);

        assertEquals(result, 3);
        assertEquals(resultShort, 3l);
        assertEquals(resultLong, 3l);
        assertEquals(resultByte, 3l);

        assertEquals(Long.valueOf(1l), springRestDemoService.testFormBody(1l));

        MultiValueMap<String, String> forms = new LinkedMultiValueMap<>();
        forms.put("form", Arrays.asList("F1"));

        assertEquals(Arrays.asList("F1"), springRestDemoService.testMapForm(forms));
        assertEquals(User.getInstance(), springRestDemoService.testJavaBeanBody(User.getInstance()));
    }

    private static void assertEquals(Object returnStr, Object exception) {
        boolean equal = returnStr != null && returnStr.equals(exception);

        if (equal) {
            return;
        } else {
            throw new RuntimeException();
        }
    }
}
