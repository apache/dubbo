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
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.reference.ReferenceBeanBuilder;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaConfigReferenceBeanTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void tearDown() {
        DubboBootstrap.reset();
    }


    @Test
    public void testAnnotationBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfig.class, AnnotationBeanConfiguration.class);

        Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
        Assertions.assertEquals(1, helloServiceMap.size());
        Assertions.assertNotNull(helloServiceMap.get("helloService"));

        Map<String, GenericService> genericServiceMap = context.getBeansOfType(GenericService.class);
        Assertions.assertEquals(1, genericServiceMap.size());
        Assertions.assertNotNull(genericServiceMap.get("genericHelloService"));

        Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
        Assertions.assertEquals(2, referenceBeanMap.size());
        ReferenceBean referenceBean = referenceBeanMap.get("&helloService");
        Assertions.assertEquals("demo", referenceBean.getGroup());
        Assertions.assertEquals(HelloService.class, referenceBean.getInterfaceClass());
        Assertions.assertEquals(HelloService.class, referenceBean.getActualInterface());

        ReferenceBean genericHelloServiceReferenceBean = referenceBeanMap.get("&genericHelloService");
        Assertions.assertEquals("demo", genericHelloServiceReferenceBean.getGroup());
        Assertions.assertEquals(GenericService.class, genericHelloServiceReferenceBean.getInterfaceClass());
        Assertions.assertEquals(HelloService.class, genericHelloServiceReferenceBean.getActualInterface());

        context.close();
    }

    @Test
    public void testGenericReferenceBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfig.class, ReferenceBeanConfiguration.class);

        Map<String, HelloService> helloServiceMap = context.getBeansOfType(HelloService.class);
        Assertions.assertEquals(2, helloServiceMap.size());
        Assertions.assertNotNull(helloServiceMap.get("helloService"));
        Assertions.assertNotNull(helloServiceMap.get("helloService2"));

        Map<String, GenericService> genericServiceMap = context.getBeansOfType(GenericService.class);
        Assertions.assertEquals(1, genericServiceMap.size());
        Assertions.assertNotNull(genericServiceMap.get("genericHelloService"));

        Map<String, ReferenceBean> referenceBeanMap = context.getBeansOfType(ReferenceBean.class);
        Assertions.assertEquals(3, referenceBeanMap.size());
        ReferenceBean referenceBean = referenceBeanMap.get("&helloService");
        Assertions.assertEquals(HelloService.class, referenceBean.getInterfaceClass());
        Assertions.assertEquals(HelloService.class, referenceBean.getActualInterface());

        ReferenceBean genericHelloServiceReferenceBean = referenceBeanMap.get("&genericHelloService");
        Assertions.assertEquals("demo", genericHelloServiceReferenceBean.getGroup());
        Assertions.assertEquals(GenericService.class, genericHelloServiceReferenceBean.getInterfaceClass());
        Assertions.assertEquals(HelloService.class, genericHelloServiceReferenceBean.getActualInterface());

        context.close();
    }

    @Test
    public void testRawReferenceBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(ConsumerConfig.class, ReferenceBeanWithoutGenericTypeConfiguration.class);
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
    public void testInconsistentBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(ConsumerConfig.class, InconsistentBeanConfiguration.class);
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
    public void testMissingGenericTypeBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(ConsumerConfig.class, MissingGenericTypeAnnotationBeanConfiguration.class);
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
    public void testMissingInterfaceTypeBean() {
        AnnotationConfigApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(ConsumerConfig.class, MissingInterfaceTypeAnnotationBeanConfiguration.class);
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
    public static class ConsumerConfig {

        @Bean
        public List<String> testBean(HelloService helloService) {
            return Arrays.asList(helloService.getClass().getName());
        }
    }

    @Configuration
    public static class AnnotationBeanConfiguration {

        @Bean
        @DubboReference(group = "${myapp.group}", init = false)
        public ReferenceBean<HelloService> helloService() {
            return new ReferenceBean();
        }

        @Bean
        @Reference(group = "${myapp.group}", interfaceClass = HelloService.class, init = false)
        public ReferenceBean<GenericService> genericHelloService() {
            return new ReferenceBean();
        }

    }

    @Configuration
    public static class ReferenceBeanConfiguration {

        @Bean
        public ReferenceBean<HelloService> helloService() {
            return new ReferenceBeanBuilder()
                    .setGroup("${myapp.group}")
                    .setInit(false)
                    .build();
        }

        @Bean
        public ReferenceBean<HelloService> helloService2() {
            return new ReferenceBeanBuilder()
                    .setInit(false)
                    .build();
        }

        @Bean
        public ReferenceBean<GenericService> genericHelloService() {
            return new ReferenceBeanBuilder()
                    .setGroup("${myapp.group}")
                    .setInterface(HelloService.class)
                    .setInit(false)
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
