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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ReferenceBeanManager;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor.BEAN_NAME;
import static org.junit.Assert.assertTrue;

/**
 * {@link ReferenceAnnotationBeanPostProcessor} Test
 *
 * @since 2.5.7
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {
                ServiceAnnotationTestConfiguration.class,
                ReferenceAnnotationBeanPostProcessorTest.class,
                ReferenceAnnotationBeanPostProcessorTest.TestAspect.class
        })
@TestPropertySource(properties = {
        "packagesToScan = org.apache.dubbo.config.spring.context.annotation.provider",
        "consumer.version = ${demo.service.version}",
        "consumer.url = dubbo://127.0.0.1:12345?version=2.5.7",
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class ReferenceAnnotationBeanPostProcessorTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
    }

    private static final String AOP_SUFFIX = "(based on AOP)";

    @Aspect
    @Component
    public static class TestAspect {

        @Around("execution(* org.apache.dubbo.config.spring.context.annotation.provider.DemoServiceImpl.*(..))")
        public Object aroundApi(ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed() + AOP_SUFFIX;
        }
    }

    @Bean
    public TestBean testBean() {
        return new TestBean();
    }

    @Bean(ReferenceBeanManager.BEAN_NAME)
    public ReferenceBeanManager referenceBeanManager() {
        return new ReferenceBeanManager();
    }

    @Bean(BEAN_NAME)
    public ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor() {
        return new ReferenceAnnotationBeanPostProcessor();
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    @Qualifier("defaultHelloService")
    private HelloService defaultHelloService;

    @Autowired
    @Qualifier("helloServiceImpl")
    private HelloService helloServiceImpl;

    // #4 ReferenceBean (Field Injection #2)
    @Reference(id = "helloService", methods = @Method(name = "sayHello", timeout = 100))
    private HelloService helloService;

    // #5 ReferenceBean (Field Injection #3)
    @Reference
    private HelloService helloService2;

    @Test
    public void testAop() throws Exception {

        assertTrue(context.containsBean("helloService"));

        TestBean testBean = context.getBean(TestBean.class);

        DemoService demoService = testBean.getDemoService();
        Map<String, DemoService> demoServicesMap = context.getBeansOfType(DemoService.class);

        Assert.assertNotNull(testBean.getDemoServiceFromAncestor());
        Assert.assertNotNull(testBean.getDemoServiceFromParent());
        Assert.assertNotNull(testBean.getDemoService());
        Assert.assertNotNull(testBean.autowiredDemoService);
        Assert.assertEquals(3, demoServicesMap.size());
        Assert.assertNotNull(demoServicesMap.get("demoServiceImpl"));
        Assert.assertNotNull(demoServicesMap.get("my-reference-bean"));
        Assert.assertNotNull(demoServicesMap.get("@Reference(url=dubbo://127.0.0.1:12345?version=2.5.7,version=2.5.7) org.apache.dubbo.config.spring.api.DemoService"));

        String expectedResult = "Hello,Mercy" + AOP_SUFFIX;

        Assert.assertEquals(expectedResult, testBean.autowiredDemoService.sayName("Mercy"));
        Assert.assertEquals(expectedResult, demoService.sayName("Mercy"));
        Assert.assertEquals("Greeting, Mercy", defaultHelloService.sayHello("Mercy"));
        Assert.assertEquals("Hello, Mercy", helloServiceImpl.sayHello("Mercy"));
        Assert.assertEquals("Greeting, Mercy", helloService.sayHello("Mercy"));


        Assert.assertEquals(expectedResult, testBean.getDemoServiceFromAncestor().sayName("Mercy"));
        Assert.assertEquals(expectedResult, testBean.getDemoServiceFromParent().sayName("Mercy"));
        Assert.assertEquals(expectedResult, testBean.getDemoService().sayName("Mercy"));

        DemoService myDemoService = context.getBean("my-reference-bean", DemoService.class);

        Assert.assertEquals(expectedResult, myDemoService.sayName("Mercy"));

    }

    @Test
    public void testGetInjectedFieldReferenceBeanMap() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
                beanPostProcessor.getInjectedFieldReferenceBeanMap();

        Assert.assertEquals(3, referenceBeanMap.size());

        Map<String, Integer> checkingFieldNames = new HashMap<>();
        checkingFieldNames.put("helloService", 0);
        checkingFieldNames.put("helloService2", 0);
        checkingFieldNames.put("demoServiceFromParent", 0);

        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {

            InjectionMetadata.InjectedElement injectedElement = entry.getKey();
            Field field = (Field) injectedElement.getMember();
            Integer count = checkingFieldNames.get(field.getName());
            Assert.assertNotNull(count);
            Assert.assertEquals(0, count.intValue());
            checkingFieldNames.put(field.getName(), count+1);
        }

        for (Map.Entry<String, Integer> entry : checkingFieldNames.entrySet()) {
            Assert.assertEquals("check field element failed: "+entry.getKey(), 1, entry.getValue().intValue());
        }
    }

    @Test
    public void testGetInjectedMethodReferenceBeanMap() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
                beanPostProcessor.getInjectedMethodReferenceBeanMap();

        Assert.assertEquals(2, referenceBeanMap.size());

        Map<String, Integer> checkingMethodNames = new HashMap<>();
        checkingMethodNames.put("setDemoServiceFromAncestor", 0);
        checkingMethodNames.put("setDemoService", 0);

        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {

            InjectionMetadata.InjectedElement injectedElement = entry.getKey();
            java.lang.reflect.Method method = (java.lang.reflect.Method) injectedElement.getMember();
            Integer count = checkingMethodNames.get(method.getName());
            Assert.assertNotNull(count);
            Assert.assertEquals(0, count.intValue());
            checkingMethodNames.put(method.getName(), count+1);
        }

        for (Map.Entry<String, Integer> entry : checkingMethodNames.entrySet()) {
            Assert.assertEquals("check method element failed: "+entry.getKey(), 1, entry.getValue().intValue());
        }
    }


    //    @Test
    //    public void testModuleInfo() {
    //
    //        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
    //                ReferenceAnnotationBeanPostProcessor.class);
    //
    //
    //        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
    //                beanPostProcessor.getInjectedMethodReferenceBeanMap();
    //
    //        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {
    //            ReferenceBean<?> referenceBean = entry.getValue();
    //
    //            assertThat(referenceBean.getModule().getName(), is("defaultModule"));
    //            assertThat(referenceBean.getMonitor(), not(nullValue()));
    //        }
    //    }

    private static class AncestorBean {

        private DemoService demoServiceFromAncestor;

        @Autowired
        private ApplicationContext applicationContext;

        public DemoService getDemoServiceFromAncestor() {
            return demoServiceFromAncestor;
        }

        // #1 ReferenceBean (Method Injection #1)
        @Reference(id = "my-reference-bean", version = "2.5.7", url = "dubbo://127.0.0.1:12345?version=2.5.7")
        public void setDemoServiceFromAncestor(DemoService demoServiceFromAncestor) {
            this.demoServiceFromAncestor = demoServiceFromAncestor;
        }

        public ApplicationContext getApplicationContext() {
            return applicationContext;
        }

    }

    private static class ParentBean extends AncestorBean {

        // #2 ReferenceBean (Field Injection #1)
        @Reference(version = "${consumer.version}", url = "${consumer.url}")
        private DemoService demoServiceFromParent;

        public DemoService getDemoServiceFromParent() {
            return demoServiceFromParent;
        }

    }

    static class TestBean extends ParentBean {

        private DemoService demoService;

        @Autowired
        private DemoService autowiredDemoService;

        @Autowired
        private ApplicationContext applicationContext;

        public DemoService getDemoService() {
            return demoService;
        }

        // #3 ReferenceBean (Method Injection #2)
        @com.alibaba.dubbo.config.annotation.Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345?version=2.5.7")
        public void setDemoService(DemoService demoService) {
            this.demoService = demoService;
        }
    }

    @Test
    public void testReferenceBeansMethodAnnotation() {

        ReferenceBeanManager referenceBeanManager = context.getBean(ReferenceBeanManager.BEAN_NAME,
                ReferenceBeanManager.class);

        Collection<ReferenceBean> referenceBeans = referenceBeanManager.getReferences();

        Assert.assertEquals(4, referenceBeans.size());

        for (ReferenceBean referenceBean : referenceBeans) {
            ReferenceConfig referenceConfig = referenceBean.getReferenceConfig();
            Assert.assertNotNull(referenceConfig);
            Assert.assertNotNull(ReferenceConfigCache.getCache().get(referenceConfig));
        }

        ReferenceBean referenceBean = referenceBeanManager.get("helloService");
        if ("helloService".equals(referenceBean.getId())) {
            ReferenceConfig referenceConfig = referenceBean.getReferenceConfig();
            Assert.assertNotNull(referenceConfig.getMethods());
        }
    }

}