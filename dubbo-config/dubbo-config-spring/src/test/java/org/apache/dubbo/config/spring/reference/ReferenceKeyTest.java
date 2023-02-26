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
package org.apache.dubbo.config.spring.reference;

import com.alibaba.spring.util.AnnotationUtils;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.api.ProvidedByDemoService1;
import org.apache.dubbo.config.spring.api.ProvidedByDemoService2;
import org.apache.dubbo.config.spring.api.ProvidedByDemoService3;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.AnnotationAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ReferenceKeyTest {

    @BeforeEach
    protected void setUp() {
        DubboBootstrap.reset();
    }

    @Test
    void testReferenceKey() throws Exception {

        String helloService1 = getReferenceKey("helloService");
        String helloService2 = getReferenceKey("helloService2");
        String helloService3 = getReferenceKey("helloService3");
        String helloService4 = getReferenceKey("helloService4");

        Assertions.assertEquals("ReferenceBean:org.apache.dubbo.config.spring.api.HelloService(methods=[{name=sayHello, retries=0, timeout=100}])",
                helloService1);
        Assertions.assertEquals(helloService1, helloService2);

        Assertions.assertEquals("ReferenceBean:org.apache.dubbo.config.spring.api.HelloService(methods=[{arguments=[{callback=true, index=0}], name=sayHello, timeout=100}])",
                helloService3);
        Assertions.assertEquals(helloService3, helloService4);


        String helloServiceWithArray0 = getReferenceKey("helloServiceWithArray0");
        String helloServiceWithArray1 = getReferenceKey("helloServiceWithArray1");
        String helloServiceWithArray2 = getReferenceKey("helloServiceWithArray2");

        String helloServiceWithMethod1 = getReferenceKey("helloServiceWithMethod1");
        String helloServiceWithMethod2 = getReferenceKey("helloServiceWithMethod2");

        String helloServiceWithArgument1 = getReferenceKey("helloServiceWithArgument1");
        String helloServiceWithArgument2 = getReferenceKey("helloServiceWithArgument2");

        Assertions.assertEquals("ReferenceBean:org.apache.dubbo.config.spring.api.HelloService(check=false,filter=[echo],parameters={a=2, b=1})",
                helloServiceWithArray0);
        Assertions.assertNotEquals(helloServiceWithArray0, helloServiceWithArray1);

        Assertions.assertEquals("ReferenceBean:org.apache.dubbo.config.spring.api.HelloService(check=false,filter=[echo],parameters={a=1, b=2})",
                helloServiceWithArray1);
        Assertions.assertEquals(helloServiceWithArray1, helloServiceWithArray2);

        Assertions.assertEquals("ReferenceBean:org.apache.dubbo.config.spring.api.HelloService(check=false,filter=[echo],methods=[{name=sayHello, parameters={c=1, d=2}, timeout=100}],parameters={a=1, b=2})",
                helloServiceWithMethod1);
        Assertions.assertEquals(helloServiceWithMethod1, helloServiceWithMethod2);

        Assertions.assertEquals("ReferenceBean:org.apache.dubbo.config.spring.api.HelloService(check=false,filter=[echo],methods=[{arguments=[{callback=true, type=String}, {type=int}], name=sayHello, timeout=100}],parameters={a=1, b=2})",
                helloServiceWithArgument1);
        Assertions.assertEquals(helloServiceWithArgument1, helloServiceWithArgument2);

    }


    @Test
    void testConfig() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        context.start();

        Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
        Assertions.assertEquals(2, referenceBeanMap.size());
        Assertions.assertEquals("ReferenceBean:demo/org.apache.dubbo.config.spring.api.DemoService:1.2.3(consumer=my-consumer,init=false,methods=[{arguments=[{callback=true, index=0}], name=sayName, parameters={access-token=my-token, b=2}, retries=0}],parameters={connec.timeout=1000},protocol=dubbo,registryIds=my-registry,scope=remote,timeout=1000,url=dubbo://127.0.0.1:20813)",
                referenceBeanMap.get("&demoService").getKey());

    }

    @Test
    @Disabled("support multi reference config")
    public void testConfig2() {
        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration2.class);
            context.start();
            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.fail("Reference bean check failed");
        } catch (BeansException e) {
            String msg = getStackTrace(e);
            Assertions.assertTrue(msg.contains("Found multiple ReferenceConfigs with unique service name [demo/org.apache.dubbo.config.spring.api.DemoService:1.2.3]"), msg);
//            Assertions.assertTrue(msg.contains("Already exists another reference bean with the same bean name and type but difference attributes"), msg);
//            Assertions.assertTrue(msg.contains("ConsumerConfiguration2.demoService"), msg);
        }
    }

    @Test
    void testConfig3() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration3.class);
        context.start();
        Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
        Assertions.assertEquals(3, referenceBeanMap.size());
        Assertions.assertNotNull(referenceBeanMap.get("&demoService#2"));

        ConsumerConfiguration3 consumerConfiguration3 = context.getBean(ConsumerConfiguration3.class);
        Assertions.assertEquals(consumerConfiguration3.demoService, consumerConfiguration3.helloService);
    }

    @Test
    void testConfig4() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(ConsumerConfiguration4.class);
            context.start();
            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.fail("Reference bean check failed");
        } catch (BeansException e) {
            String msg = getStackTrace(e);
            Assertions.assertTrue(msg.contains("Duplicate spring bean name: demoService"), msg);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    @Test
    void testConfig5() {
        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration5.class);
            context.start();
            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.fail("Reference bean check failed");
        } catch (BeansException e) {
            Assertions.assertTrue(getStackTrace(e).contains("Duplicate spring bean name: demoService"));
        }
    }

    @Test
    void testConfig6() {
        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration6.class);
            context.start();
            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.fail("Reference bean check failed");
        } catch (BeansException e) {
            String checkString = "Already exists another bean definition with the same bean name [demoService], but cannot rename the reference bean name";
            String msg = getStackTrace(e);
            Assertions.assertTrue(msg.contains(checkString), msg);
            Assertions.assertTrue(msg.contains("ConsumerConfiguration6.demoService"), msg);
        }
    }

    @Test
    void testConfig7() throws Exception{
        String fieldName1 = "providedByDemoService1";
        String fieldName2 = "providedByDemoService2";
        String fieldName3 = "providedByDemoServiceInterface";
        String fieldName4 = "multiProvidedByDemoServiceInterface";
        Map<String, Object> attributes1= getReferenceAttributes(fieldName1);
        Map<String, Object> attributes2= getReferenceAttributes(fieldName2);
        Map<String, Object> attributes3= getReferenceAttributes(fieldName3);
        Map<String, Object> attributes4= getReferenceAttributes(fieldName4);

        Assertions.assertEquals("provided-demo-service-interface", ((String[])attributes1.get("providedBy"))[0]);
        Assertions.assertEquals("provided-demo-service1", ((String[])attributes1.get("providedBy"))[1]);
        Assertions.assertEquals("provided-demo-service2", ((String[]) attributes2.get("providedBy"))[0]);
        Assertions.assertEquals("provided-demo-service-interface", ((String[]) attributes3.get("providedBy"))[0]);
        String[] serviceName4 = (String[]) attributes4.get("providedBy");
        List<String> expectServices = new ArrayList<>();
        expectServices.add("provided-demo-service-interface1");
        expectServices.add("provided-demo-service-interface2");
        Assertions.assertTrue(serviceName4.length == 2 && expectServices.contains(serviceName4[0]) && expectServices.contains(serviceName4[1]));
        Assertions.assertEquals("provided-demo-service-interface", ((String[]) attributes3.get("providedBy"))[0]);
    }

    private String getStackTrace(Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private String getReferenceKey(String fieldName) throws NoSuchFieldException {
        Field field = ReferenceConfiguration.class.getDeclaredField(fieldName);
        AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes(field, DubboReference.class, null, true);
        ReferenceBeanSupport.convertReferenceProps(attributes, field.getType());
        return ReferenceBeanSupport.generateReferenceKey(attributes, null);
    }

    private Map<String, Object> getReferenceAttributes(String fieldName) throws NoSuchFieldException {
        Field field = ConsumerConfiguration7.class.getDeclaredField(fieldName);
        AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes(field, DubboReference.class, null, true);
        ReferenceBeanSupport.convertReferenceProps(attributes, field.getType());
        return attributes;
    }

    static class ReferenceConfiguration {
        @DubboReference(methods = @Method(name = "sayHello", timeout = 100, retries = 0))
        private HelloService helloService;

        @DubboReference(methods = @Method(timeout = 100, name = "sayHello", retries = 0))
        private HelloService helloService2;

        @DubboReference(methods = @Method(name = "sayHello", timeout = 100, arguments = @Argument(index = 0, callback = true)))
        private HelloService helloService3;

        @DubboReference(methods = @Method(arguments = @Argument(callback = true, index = 0), name = "sayHello", timeout = 100))
        private HelloService helloService4;

        // Instance 1
        @DubboReference(check = false, parameters = {"a", "2", "b", "1"}, filter = {"echo"})
        private HelloService helloServiceWithArray0;

        // Instance 2
        @DubboReference(check = false, parameters = {"a=1", "b", "2"}, filter = {"echo"})
        private HelloService helloServiceWithArray1;

        @DubboReference(parameters = {"b", "2", "a", "1"}, filter = {"echo"}, check = false)
        private HelloService helloServiceWithArray2;

        // Instance 3
        @DubboReference(check = false, parameters = {"a", "1", "b", "2"}, filter = {"echo"}, methods = {@Method(parameters = {"d", "2", "c", "1"}, name = "sayHello", timeout = 100)})
        private HelloService helloServiceWithMethod1;

        @DubboReference(parameters = {"b=2", "a=1"}, filter = {"echo"}, check = false, methods = {@Method(name = "sayHello", timeout = 100, parameters = {"c", "1", "d", "2"})})
        private HelloService helloServiceWithMethod2;

        // Instance 4
        @DubboReference(parameters = {"a", "1", "b", "2"}, filter = {"echo"}, methods = {@Method(name = "sayHello", arguments = {@Argument(callback = true, type = "String"), @Argument(callback = false, type = "int")}, timeout = 100)}, check = false)
        private HelloService helloServiceWithArgument1;

        @DubboReference(check = false, filter = {"echo"}, parameters = {"b", "2", "a", "1"}, methods = {@Method(name = "sayHello", timeout = 100, arguments = {@Argument(callback = false, type = "int"), @Argument(callback = true, type = "String")})})
        private HelloService helloServiceWithArgument2;
    }

    @Configuration
    @ImportResource({"classpath:/org/apache/dubbo/config/spring/init-reference-keys.xml",
            "classpath:/org/apache/dubbo/config/spring/init-reference-properties.xml"})
    static class ConsumerConfiguration {

        //both are reference beans, same as xml config
        @DubboReference(group = "demo", version = "1.2.3", consumer="my-consumer", init=false,
                methods={@Method(arguments={@Argument(callback=true, index=0)}, name="sayName", parameters={"access-token", "my-token", "b", "2"}, retries=0)},
                parameters={"connec.timeout", "1000"},
                protocol="dubbo",
                registry="my-registry",
                scope="remote",
                timeout=1000,
                url="dubbo://127.0.0.1:20813")
        private DemoService demoService;
    }


    @Configuration
    @ImportResource({"classpath:/org/apache/dubbo/config/spring/init-reference-keys.xml",
            "classpath:/org/apache/dubbo/config/spring/init-reference-properties.xml"})
    static class ConsumerConfiguration2 {

        //both are reference beans, same bean name and type, but difference attributes from xml config
        @DubboReference(group = "demo", version = "1.2.3", consumer="my-consumer", init=false,
                scope="local",
                timeout=100)
        private DemoService demoService;
    }

    @Configuration
    @ImportResource({"classpath:/org/apache/dubbo/config/spring/init-reference-keys.xml",
            "classpath:/org/apache/dubbo/config/spring/init-reference-properties.xml"})
    static class ConsumerConfiguration3 {

        //both are reference beans, same bean name but difference interface type
        @DubboReference(group = "demo", version = "1.2.4", consumer="my-consumer", init=false,
                url="dubbo://127.0.0.1:20813")
        private HelloService demoService;

        @Autowired
        private HelloService helloService;
    }

    @Configuration
    @ImportResource({"classpath:/org/apache/dubbo/config/spring/init-reference-keys.xml",
            "classpath:/org/apache/dubbo/config/spring/init-reference-properties.xml"})
    static class ConsumerConfiguration4 {

        //not reference bean: same bean name and type
        @Bean
        public DemoService demoService() {
            return new DemoServiceImpl();
        }
    }

    @Configuration
    @ImportResource({"classpath:/org/apache/dubbo/config/spring/init-reference-keys.xml",
            "classpath:/org/apache/dubbo/config/spring/init-reference-properties.xml"})
    static class ConsumerConfiguration5 {

        //not reference bean: same bean name but difference type
        @Bean
        public HelloService demoService() {
            return new HelloServiceImpl();
        }
    }

    @Configuration
    @ImportResource({"classpath:/org/apache/dubbo/config/spring/init-reference-keys.xml",
            "classpath:/org/apache/dubbo/config/spring/init-reference-properties.xml"})
    static class ConsumerConfiguration6 {

        //both are reference beans, same bean name but difference interface type, fixed bean name
        @DubboReference(id = "demoService", group = "demo", version = "1.2.3", consumer="my-consumer", init=false,
                url="dubbo://127.0.0.1:20813")
        private HelloService demoService;

//        @Autowired
//        private HelloService helloService;
    }

    @Configuration
    static class ConsumerConfiguration7 {

        //both are reference beans, same as xml config
        @DubboReference(providedBy = "provided-demo-service1")
        private ProvidedByDemoService1 providedByDemoService1;

        @DubboReference(providedBy = "provided-demo-service2")
        private ProvidedByDemoService2 providedByDemoService2;

        @DubboReference(providedBy = {"provided-demo-service3", "provided-demo-service4"})
        private ProvidedByDemoService2 multiProvidedByDemoService;

        @DubboReference
        private ProvidedByDemoService1 providedByDemoServiceInterface;

        @DubboReference
        private ProvidedByDemoService3 multiProvidedByDemoServiceInterface;
    }

}
