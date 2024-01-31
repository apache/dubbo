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

import org.apache.dubbo.demo.rest.api.annotation.DubboServiceAnnotationServiceConsumer;

import java.util.Arrays;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import po.User;

public class RestConsumer {

    public static void main(String[] args) {
        consumerService();
    }

    public static void consumerService() {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(new String[] {"spring/rest-consumer.xml"});
        context.start();
        System.out.println("rest consumer start");
        testExceptionMapperService(context);
        testHttpMethodService(context);
        httpRPCContextTest(context);
        jaxRsRestDemoServiceTest(context);
        annotationTest(context);
        System.out.println("rest consumer test success");
    }

    private static void annotationTest(ClassPathXmlApplicationContext context) {
        DubboServiceAnnotationServiceConsumer bean = context.getBean(DubboServiceAnnotationServiceConsumer.class);
        bean.invokeAnnotationService();
    }

    private static void jaxRsRestDemoServiceTest(ClassPathXmlApplicationContext context) {
        JaxRsRestDemoService jaxRsRestDemoService = context.getBean("jaxRsRestDemoService", JaxRsRestDemoService.class);
        String hello = jaxRsRestDemoService.sayHello("hello");
        assertEquals("Hello, hello", hello);
        Integer result = jaxRsRestDemoService.primitiveInt(1, 2);
        Long resultLong = jaxRsRestDemoService.primitiveLong(1, 2l);
        long resultByte = jaxRsRestDemoService.primitiveByte((byte) 1, 2l);
        long resultShort = jaxRsRestDemoService.primitiveShort((short) 1, 2l, 1);

        assertEquals(result, 3);
        assertEquals(resultShort, 3l);
        assertEquals(resultLong, 3l);
        assertEquals(resultByte, 3l);

        assertEquals(Long.valueOf(1l), jaxRsRestDemoService.testFormBody(1l));

        MultivaluedMapImpl<String, String> forms = new MultivaluedMapImpl<>();
        forms.put("form", Arrays.asList("F1"));

        assertEquals(Arrays.asList("F1"), jaxRsRestDemoService.testMapForm(forms));
        assertEquals(User.getInstance(), jaxRsRestDemoService.testJavaBeanBody(User.getInstance()));
    }

    private static void testExceptionMapperService(ClassPathXmlApplicationContext context) {
        String returnStr = "exception";
        String paramStr = "exception";
        ExceptionMapperService exceptionMapperService =
                context.getBean("exceptionMapperService", ExceptionMapperService.class);
        assertEquals(returnStr, exceptionMapperService.exception(paramStr));
    }

    private static void httpRPCContextTest(ClassPathXmlApplicationContext context) {

        HttpRequestAndResponseRPCContextService requestAndResponseRPCContextService = context.getBean(
                "httpRequestAndResponseRPCContextService", HttpRequestAndResponseRPCContextService.class);
        String returnStr = "hello";
        String paramStr = "hello";
        assertEquals(returnStr, requestAndResponseRPCContextService.httpRequestHeader(paramStr));
        assertEquals(returnStr, requestAndResponseRPCContextService.httpRequestParam(paramStr));
        assertEquals(
                returnStr,
                requestAndResponseRPCContextService.httpResponseHeader(paramStr).get(0));
    }

    private static void testHttpMethodService(ClassPathXmlApplicationContext context) {
        HttpMethodService httpMethodService = context.getBean("httpMethodService", HttpMethodService.class);
        String returnStr = "hello";
        String paramStr = "hello";
        //        assertEquals(null, httpMethodService.sayHelloHead(paramStr));
        assertEquals(returnStr, httpMethodService.sayHelloGet(paramStr));
        assertEquals(returnStr, httpMethodService.sayHelloDelete(paramStr));
        assertEquals(returnStr, httpMethodService.sayHelloPut(paramStr));
        assertEquals(returnStr, httpMethodService.sayHelloOptions(paramStr));
        //        Assert.assertEquals(returnStr, httpMethodService.sayHelloPatch(paramStr));
        assertEquals(returnStr, httpMethodService.sayHelloPost(paramStr));
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
