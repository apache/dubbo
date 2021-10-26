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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.utils.ReferenceConfigCache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collection;
import java.util.Map;

import static org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor.BEAN_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ReferenceAnnotationBeanPostProcessor} Test
 *
 * @since 2.5.7
 */
@EnableDubbo(scanBasePackages = "org.apache.dubbo.config.spring.context.annotation.provider")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                ServiceAnnotationTestConfiguration.class,
                ReferenceAnnotationBeanPostProcessorTest.class,
                ReferenceAnnotationBeanPostProcessorTest.TestAspect.class
        })
@TestPropertySource(properties = {
        "packagesToScan = nopackage",
        "consumer.version = ${demo.service.version}",
        "consumer.url = dubbo://127.0.0.1:12345?version=2.5.7",
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReferenceAnnotationBeanPostProcessorTest {

    @AfterTestMethod
    public void setUp() {
        DubboBootstrap.reset();
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
    @Reference(id="helloService2", tag = "demo_tag")
    private HelloService helloService2;

    // Instance 1
    @Reference(check = false, parameters = {"a", "2", "b", "1"}, filter = {"echo"})
    private HelloService helloServiceWithArray0;

    // Instance 2
    @Reference(check = false, parameters = {"a", "1", "b", "2"}, filter = {"echo"})
    private HelloService helloServiceWithArray1;

    @Reference(parameters = {"b", "2", "a", "1"}, filter = {"echo"}, check = false)
    private HelloService helloServiceWithArray2;

    // Instance 3
    @Reference(check = false, parameters = {"a", "1"}, filter = {"echo"}, methods = {@Method(name = "sayHello", timeout = 100)})
    private HelloService helloServiceWithMethod1;

    @Reference(parameters = {"a", "1"}, filter = {"echo"}, check = false, methods = {@Method(name = "sayHello", timeout = 100)})
    private HelloService helloServiceWithMethod2;

    // Instance 4
    @Reference(parameters = {"a", "1"}, filter = {"echo"}, methods = {@Method(name = "sayHello", arguments = {@Argument(callback = true, type = "String"), @Argument(callback = false, type = "int")}, timeout = 100)}, check = false)
    private HelloService helloServiceWithArgument1;

    @Reference(check = false, filter = {"echo"}, parameters = {"a", "1"}, methods = {@Method(name = "sayHello", timeout = 100, arguments = {@Argument(callback = false, type = "int"), @Argument(callback = true, type = "String")})})
    private HelloService helloServiceWithArgument2;

    @Test
    public void test() throws Exception {

        assertTrue(context.containsBean("helloService"));

        TestBean testBean = context.getBean(TestBean.class);

        DemoService demoService = testBean.getDemoService();
        Map<String, DemoService> demoServicesMap = context.getBeansOfType(DemoService.class);

        Assertions.assertNotNull(testBean.getDemoServiceFromAncestor());
        Assertions.assertNotNull(testBean.getDemoServiceFromParent());
        Assertions.assertNotNull(testBean.getDemoService());
        Assertions.assertNotNull(testBean.autowiredDemoService);
        Assertions.assertEquals(1, demoServicesMap.size());

        String expectedResult = "Hello,Mercy" + AOP_SUFFIX;

        Assertions.assertEquals(expectedResult, testBean.autowiredDemoService.sayName("Mercy"));
        Assertions.assertEquals(expectedResult, demoService.sayName("Mercy"));
        Assertions.assertEquals("Greeting, Mercy", defaultHelloService.sayHello("Mercy"));
        Assertions.assertEquals("Hello, Mercy", helloServiceImpl.sayHello("Mercy"));
        Assertions.assertEquals("Greeting, Mercy", helloService.sayHello("Mercy"));


        Assertions.assertEquals(expectedResult, testBean.getDemoServiceFromAncestor().sayName("Mercy"));
        Assertions.assertEquals(expectedResult, testBean.getDemoServiceFromParent().sayName("Mercy"));
        Assertions.assertEquals(expectedResult, testBean.getDemoService().sayName("Mercy"));

        DemoService myDemoService = context.getBean("my-reference-bean", DemoService.class);

        Assertions.assertEquals(expectedResult, myDemoService.sayName("Mercy"));


        for (DemoService demoService1 : demoServicesMap.values()) {

            Assertions.assertEquals(myDemoService, demoService1);

            Assertions.assertEquals(expectedResult, demoService1.sayName("Mercy"));
        }

    }

    /**
     * Test on {@link ReferenceAnnotationBeanPostProcessor#getReferenceBeans()}
     */
    @Test
    public void testGetReferenceBeans() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Collection<ReferenceBean<?>> referenceBeans = beanPostProcessor.getReferenceBeans();

        Assertions.assertEquals(8, referenceBeans.size());

        ReferenceBean<?> referenceBean = referenceBeans.iterator().next();

        Assertions.assertNotNull(ReferenceConfigCache.getCache().get(referenceBean));

        ReferenceBean helloService2Bean = getReferenceBean("helloService2", referenceBeans);
        Assertions.assertEquals("demo_tag", helloService2Bean.getTag());
    }

    private ReferenceBean getReferenceBean(String name, Collection<ReferenceBean<?>> referenceBeans) {
        for (ReferenceBean<?> referenceBean : referenceBeans) {
            if (StringUtils.isEquals(name, referenceBean.getId())) {
                return referenceBean;
            }
        }
        return null;
    }

    @Test
    public void testGetInjectedFieldReferenceBeanMap() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
                beanPostProcessor.getInjectedFieldReferenceBeanMap();

        Assertions.assertEquals(10, referenceBeanMap.size());

        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {

            InjectionMetadata.InjectedElement injectedElement = entry.getKey();

            Assertions.assertEquals("com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor$AnnotatedFieldElement",
                    injectedElement.getClass().getName());

        }

    }

    @Test
    public void testGetInjectedMethodReferenceBeanMap() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
                beanPostProcessor.getInjectedMethodReferenceBeanMap();

        Assertions.assertEquals(2, referenceBeanMap.size());

        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {

            InjectionMetadata.InjectedElement injectedElement = entry.getKey();

            Assertions.assertEquals("com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor$AnnotatedMethodElement",
                    injectedElement.getClass().getName());

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

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Collection<ReferenceBean<?>> referenceBeans = beanPostProcessor.getReferenceBeans();

        Assertions.assertEquals(8, referenceBeans.size());

        ReferenceBean<?> referenceBean = referenceBeans.iterator().next();

        if ("helloService".equals(referenceBean.getId())) {
            Assertions.assertNotNull(referenceBean.getMethods());
        }
    }

}