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
package org.apache.dubbo.config.spring.reference.javaconfig;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.config.spring.reference.ReferenceBeanBuilder;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class JavaConfigReferenceBeanTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
    }

    @Test
    void testAnnotationAtField() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CommonConfig.class,
            AnnotationAtFieldConfiguration.class);

        try {
            Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
            Assertions.assertEquals(2, helloServiceMap.size());
            Assertions.assertNotNull(helloServiceMap.get("helloService"));
            Assertions.assertNotNull(helloServiceMap.get("helloServiceImpl"));

            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(1, referenceBeanMap.size());
            ReferenceBean referenceBean = referenceBeanMap.get("&helloService");
            Assertions.assertEquals("demo", referenceBean.getGroup());
            Assertions.assertEquals(HelloService.class, referenceBean.getInterfaceClass());
            Assertions.assertEquals(HelloService.class.getName(), referenceBean.getServiceInterface());
        } finally {
            context.close();
        }
    }

    @Test
    @Disabled("support multi reference config")
    public void testAnnotationAtField2() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(CommonConfig.class,
                AnnotationAtFieldConfiguration.class, AnnotationAtFieldConfiguration2.class);
            Assertions.fail("Should not load duplicated @DubboReference fields");
        } catch (Exception e) {
            String msg = getStackTrace(e);
            Assertions.assertTrue(msg.contains("Found multiple ReferenceConfigs with unique service name [demo/org.apache.dubbo.config.spring.api.HelloService]"), msg);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    private String getStackTrace(Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    @Test
    void testAnnotationBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CommonConfig.class,
                AnnotationBeanConfiguration.class);

        try {
            Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
            Assertions.assertEquals(2, helloServiceMap.size());
            Assertions.assertNotNull(helloServiceMap.get("helloService"));
            Assertions.assertNotNull(helloServiceMap.get("helloServiceImpl"));

            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(1, referenceBeanMap.size());
            ReferenceBean referenceBean = referenceBeanMap.get("&helloService");
            Assertions.assertEquals("demo", referenceBean.getGroup());
            Assertions.assertEquals(HelloService.class, referenceBean.getInterfaceClass());
            Assertions.assertEquals(HelloService.class.getName(), referenceBean.getServiceInterface());
        } finally {
            context.close();
        }
    }

    @Test
    void testGenericServiceAnnotationBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CommonConfig.class,
            GenericServiceAnnotationBeanConfiguration.class);

        try {
            Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
            Assertions.assertEquals(1, helloServiceMap.size());
            Assertions.assertNotNull(helloServiceMap.get("helloServiceImpl"));

            Map<String, GenericService> genericServiceMap = context.getBeansOfType(GenericService.class);
            Assertions.assertEquals(3, genericServiceMap.size());
            Assertions.assertNotNull(genericServiceMap.get("genericHelloService"));

            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(2, referenceBeanMap.size());

            ReferenceBean genericHelloServiceReferenceBean = referenceBeanMap.get("&genericHelloService");
            Assertions.assertEquals("demo", genericHelloServiceReferenceBean.getGroup());
            Assertions.assertEquals(GenericService.class, genericHelloServiceReferenceBean.getInterfaceClass());
            Assertions.assertEquals(HelloService.class.getName(), genericHelloServiceReferenceBean.getServiceInterface());

            ReferenceBean genericServiceWithoutInterfaceBean = referenceBeanMap.get("&genericServiceWithoutInterface");
            Assertions.assertEquals("demo", genericServiceWithoutInterfaceBean.getGroup());
            Assertions.assertEquals(GenericService.class, genericServiceWithoutInterfaceBean.getInterfaceClass());
            Assertions.assertEquals("org.apache.dubbo.config.spring.api.LocalMissClass", genericServiceWithoutInterfaceBean.getServiceInterface());

            GenericService genericServiceWithoutInterface = context.getBean("genericServiceWithoutInterface", GenericService.class);
            Assertions.assertNotNull(genericServiceWithoutInterface);
            Object sayHelloResult = genericServiceWithoutInterface.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"Dubbo"});
            Assertions.assertEquals("Hello Dubbo", sayHelloResult);
        } finally {
            context.close();
        }

    }

    @Test
    void testReferenceBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CommonConfig.class,
                ReferenceBeanConfiguration.class);

        try {
            Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
            Assertions.assertEquals(2, helloServiceMap.size());
            Assertions.assertNotNull(helloServiceMap.get("helloService"));
            Assertions.assertNotNull(helloServiceMap.get("helloServiceImpl"));

            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(2, referenceBeanMap.size());
            ReferenceBean referenceBean = referenceBeanMap.get("&helloService");
            Assertions.assertEquals(HelloService.class, referenceBean.getInterfaceClass());
            Assertions.assertEquals(HelloService.class.getName(), referenceBean.getServiceInterface());

            ReferenceBean demoServiceReferenceBean = referenceBeanMap.get("&demoService");
            Assertions.assertEquals(DemoService.class, demoServiceReferenceBean.getInterfaceClass());
            Assertions.assertEquals(DemoService.class.getName(), demoServiceReferenceBean.getServiceInterface());
        } finally {
            context.close();
        }
    }

    @Test
    void testGenericServiceReferenceBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CommonConfig.class,
            GenericServiceReferenceBeanConfiguration.class);

        try {
            Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
            Assertions.assertEquals(1, helloServiceMap.size());
            Assertions.assertNotNull(helloServiceMap.get("helloServiceImpl"));

            Map<String, GenericService> genericServiceMap = context.getBeansOfType(GenericService.class);
            Assertions.assertEquals(2, genericServiceMap.size());
            Assertions.assertNotNull(genericServiceMap.get("localMissClassGenericServiceImpl"));
            Assertions.assertNotNull(genericServiceMap.get("genericHelloService"));

            Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
            Assertions.assertEquals(1, referenceBeanMap.size());

            ReferenceBean genericHelloServiceReferenceBean = referenceBeanMap.get("&genericHelloService");
            Assertions.assertEquals("demo", genericHelloServiceReferenceBean.getGroup());
            Assertions.assertEquals(GenericService.class, genericHelloServiceReferenceBean.getInterfaceClass());
            Assertions.assertEquals(HelloService.class.getName(), genericHelloServiceReferenceBean.getServiceInterface());
        } finally {
            context.close();
        }
    }

    @Test
    void testRawReferenceBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(CommonConfig.class, ReferenceBeanWithoutGenericTypeConfiguration.class);
            Assertions.fail("Should not load application");

        } catch (Exception e) {
            String s = e.toString();
            Assertions.assertTrue(s.contains("The ReferenceBean is missing necessary generic type"), s);
            Assertions.assertTrue(s.contains("ReferenceBeanWithoutGenericTypeConfiguration#helloService()"), s);
        } finally {
            if (context != null) {
                context.close();
            }
        }

    }

    @Test
    void testInconsistentBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(CommonConfig.class, InconsistentBeanConfiguration.class);
            Assertions.fail("Should not load application");
        } catch (Exception e) {
            String s = e.toString();
            Assertions.assertTrue(s.contains("@DubboReference annotation is inconsistent with the generic type of the ReferenceBean"), s);
            Assertions.assertTrue(s.contains("ReferenceBean<org.apache.dubbo.config.spring.api.HelloService>"), s);
            Assertions.assertTrue(s.contains("InconsistentBeanConfiguration#helloService()"), s);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    @Test
    void testMissingGenericTypeBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(CommonConfig.class, MissingGenericTypeAnnotationBeanConfiguration.class);
            Assertions.fail("Should not load application");
        } catch (Exception e) {
            String s = e.toString();
            Assertions.assertTrue(s.contains("The ReferenceBean is missing necessary generic type"), s);
            Assertions.assertTrue(s.contains("MissingGenericTypeAnnotationBeanConfiguration#helloService()"), s);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    @Test
    void testMissingInterfaceTypeBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(CommonConfig.class, MissingInterfaceTypeAnnotationBeanConfiguration.class);
            Assertions.fail("Should not load application");
        } catch (Exception e) {
            String s = e.toString();
            Assertions.assertTrue(s.contains("The interface class or name of reference was not found"), s);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    @Configuration
    @EnableDubbo
    @PropertySource("classpath:/org/apache/dubbo/config/spring/reference/javaconfig/consumer.properties")
    public static class CommonConfig {

        @Bean
        public List<String> testBean(HelloService helloService) {
            return Arrays.asList(helloService.getClass().getName());
        }

        @Bean
        @DubboService(group = "${myapp.group}")
        public HelloService helloServiceImpl() {
            return new HelloServiceImpl();
        }

        @Bean
        @DubboService(group = "${myapp.group}", interfaceName = "org.apache.dubbo.config.spring.api.LocalMissClass")
        public GenericService localMissClassGenericServiceImpl() {
            return new GenericService() {
                @Override
                public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
                    if ("sayHello".equals(method)) {
                        return "Hello " + args[0];
                    }
                    return null;
                }
            };
        }
    }


    @Configuration
    public static class AnnotationAtFieldConfiguration {

        @DubboReference(group = "${myapp.group}")
        private HelloService helloService;

    }

    @Configuration
    public static class AnnotationAtFieldConfiguration2 {

        @DubboReference(group = "${myapp.group}", timeout = 2000)
        private HelloService helloService;

    }

    @Configuration
    public static class AnnotationBeanConfiguration {

        @Bean
        @DubboReference(group = "${myapp.group}")
        public ReferenceBean<HelloService> helloService() {
            return new ReferenceBean();
        }

    }

    @Configuration
    public static class GenericServiceAnnotationBeanConfiguration {

        @Bean
        @Reference(group = "${myapp.group}", interfaceClass = HelloService.class)
        public ReferenceBean<GenericService> genericHelloService() {
            return new ReferenceBean();
        }

        @Bean
        @Reference(group = "${myapp.group}", interfaceName = "org.apache.dubbo.config.spring.api.LocalMissClass")
        public ReferenceBean<GenericService> genericServiceWithoutInterface() {
            return new ReferenceBean();
        }
    }

    @Configuration
    public static class ReferenceBeanConfiguration {

        @Bean
        public ReferenceBean<HelloService> helloService() {
            return new ReferenceBeanBuilder()
                    .setGroup("${myapp.group}")
                    .build();
        }

        @Bean
        public ReferenceBean<DemoService> demoService() {
            return new ReferenceBean();
        }
    }

    @Configuration
    public static class GenericServiceReferenceBeanConfiguration {

        @Bean
        public ReferenceBean<GenericService> genericHelloService() {
            return new ReferenceBeanBuilder()
                .setGroup("${myapp.group}")
                .setInterface(HelloService.class)
                .build();
        }
    }

    @Configuration
    public static class ReferenceBeanWithoutGenericTypeConfiguration {

        // The ReferenceBean is missing necessary generic type
        @Bean
        public ReferenceBean helloService() {
            return new ReferenceBeanBuilder()
                    .setGroup("${myapp.group}")
                    .setInterface(HelloService.class)
                    .build();
        }

    }

    @Configuration
    public static class InconsistentBeanConfiguration {

        // The 'interfaceClass' or 'interfaceName' attribute value of @DubboReference annotation is inconsistent with
        // the generic type of the ReferenceBean returned by the bean method.
        @Bean
        @DubboReference(group = "${myapp.group}", interfaceClass = DemoService.class)
        public ReferenceBean<HelloService> helloService() {
            return new ReferenceBean();
        }
    }

    @Configuration
    public static class MissingGenericTypeAnnotationBeanConfiguration {

        // The ReferenceBean is missing necessary generic type
        @Bean
        @DubboReference(group = "${myapp.group}", interfaceClass = DemoService.class)
        public ReferenceBean helloService() {
            return new ReferenceBean();
        }
    }

    @Configuration
    public static class MissingInterfaceTypeAnnotationBeanConfiguration {

        // The ReferenceBean is missing necessary generic type
        @Bean
        @DubboReference(group = "${myapp.group}")
        public ReferenceBean<GenericService> helloService() {
            return new ReferenceBean();
        }
    }

}