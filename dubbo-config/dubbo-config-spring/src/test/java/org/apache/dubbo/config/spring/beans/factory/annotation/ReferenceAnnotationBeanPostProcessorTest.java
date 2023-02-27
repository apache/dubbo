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

<<<<<<< HEAD
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Argument;
=======
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.DubboReference;
>>>>>>> origin/3.2
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
<<<<<<< HEAD
import org.apache.dubbo.config.utils.ReferenceConfigCache;
=======
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.RpcContext;
>>>>>>> origin/3.2

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
<<<<<<< HEAD
import org.junit.jupiter.api.Assertions;
=======
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
>>>>>>> origin/3.2
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
<<<<<<< HEAD
import org.springframework.test.context.event.annotation.AfterTestMethod;
=======
>>>>>>> origin/3.2
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

<<<<<<< HEAD
import static org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor.BEAN_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
=======
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
>>>>>>> origin/3.2

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
                ReferenceAnnotationBeanPostProcessorTest.MyConfiguration.class,
                ReferenceAnnotationBeanPostProcessorTest.TestAspect.class
        })
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
<<<<<<< HEAD
        "packagesToScan = nopackage",
=======
>>>>>>> origin/3.2
        "consumer.version = ${demo.service.version}",
        "consumer.url = dubbo://127.0.0.1:12345?version=2.5.7",
})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
<<<<<<< HEAD
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReferenceAnnotationBeanPostProcessorTest {

    @AfterTestMethod
    public void setUp() {
=======
class ReferenceAnnotationBeanPostProcessorTest {

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
>>>>>>> origin/3.2
        DubboBootstrap.reset();
    }

    private static final String AOP_SUFFIX = "(based on AOP)";

    @Aspect
    @Component
    public static class TestAspect {

        @Around("execution(* org.apache.dubbo.config.spring.context.annotation.provider.DemoServiceImpl.*(..))")
        public Object aroundDemoService(ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed() + AOP_SUFFIX + " from " + RpcContext.getContext().getLocalAddress();
        }

        @Around("execution(* org.apache.dubbo.config.spring.context.annotation.provider.*HelloService*.*(..))")
        public Object aroundHelloService(ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed() + AOP_SUFFIX + " from " + RpcContext.getContext().getLocalAddress();
        }

<<<<<<< HEAD
=======
    }

>>>>>>> origin/3.2
    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private HelloService defaultHelloService;

    @Autowired
    private HelloService helloServiceImpl;

    @Autowired
    private DemoService demoServiceImpl;

    // #5 ReferenceBean (Field Injection #3)
    @Reference(id = "helloService", methods = @Method(name = "sayHello", timeout = 100))
    private HelloService helloService;

<<<<<<< HEAD
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
=======
    // #6 ReferenceBean (Field Injection #4)
    @DubboReference(version = "2", url = "dubbo://127.0.0.1:12345?version=2", tag = "demo_tag")
    private HelloService helloService2;

    // #7 ReferenceBean (Method Injection #3)
    @DubboReference(version = "3", url = "dubbo://127.0.0.1:12345?version=2", tag = "demo_tag")
    public void setHelloService2(HelloService helloService2) {
        // The helloService2 beanName is the same as above(#6 ReferenceBean (Field Injection #4)), and this will rename to helloService2#2
        renamedHelloService2 = helloService2;
    }

    private HelloService renamedHelloService2;
>>>>>>> origin/3.2

    @Test
    void testAop() throws Exception {

        Assertions.assertTrue(context.containsBean("helloService"));

        TestBean testBean = context.getBean(TestBean.class);

        Map<String, DemoService> demoServicesMap = context.getBeansOfType(DemoService.class);

        Assertions.assertNotNull(testBean.getDemoServiceFromAncestor());
        Assertions.assertNotNull(testBean.getDemoServiceFromParent());
        Assertions.assertNotNull(testBean.getDemoService());
<<<<<<< HEAD
        Assertions.assertNotNull(testBean.autowiredDemoService);
        Assertions.assertEquals(1, demoServicesMap.size());

        String expectedResult = "Hello,Mercy" + AOP_SUFFIX;

        Assertions.assertEquals(expectedResult, testBean.autowiredDemoService.sayName("Mercy"));
        Assertions.assertEquals(expectedResult, demoService.sayName("Mercy"));
        Assertions.assertEquals("Greeting, Mercy", defaultHelloService.sayHello("Mercy"));
        Assertions.assertEquals("Hello, Mercy", helloServiceImpl.sayHello("Mercy"));
        Assertions.assertEquals("Greeting, Mercy", helloService.sayHello("Mercy"));
=======
        Assertions.assertNotNull(testBean.myDemoService);
        Assertions.assertEquals(2, demoServicesMap.size());

        Assertions.assertNotNull(context.getBean("demoServiceImpl"));
        Assertions.assertNotNull(context.getBean("myDemoService"));
        Assertions.assertNotNull(context.getBean("demoService"));
        Assertions.assertNotNull(context.getBean("demoServiceFromParent"));
>>>>>>> origin/3.2

        String callSuffix = AOP_SUFFIX + " from "+ InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 12345);
        String localCallSuffix = AOP_SUFFIX + " from " + InetSocketAddress.createUnresolved("127.0.0.1", 0);
        String directInvokeSuffix = AOP_SUFFIX + " from null";

<<<<<<< HEAD
        Assertions.assertEquals(expectedResult, testBean.getDemoServiceFromAncestor().sayName("Mercy"));
        Assertions.assertEquals(expectedResult, testBean.getDemoServiceFromParent().sayName("Mercy"));
        Assertions.assertEquals(expectedResult, testBean.getDemoService().sayName("Mercy"));
=======
        String defaultHelloServiceResult = "Greeting, Mercy";
        Assertions.assertEquals(defaultHelloServiceResult + directInvokeSuffix, defaultHelloService.sayHello("Mercy"));
        Assertions.assertEquals(defaultHelloServiceResult + localCallSuffix, helloService.sayHello("Mercy"));
>>>>>>> origin/3.2

        String helloServiceImplResult = "Hello, Mercy";
        Assertions.assertEquals(helloServiceImplResult + directInvokeSuffix, helloServiceImpl.sayHello("Mercy"));
        Assertions.assertEquals(helloServiceImplResult + callSuffix, helloService2.sayHello("Mercy"));

<<<<<<< HEAD
        Assertions.assertEquals(expectedResult, myDemoService.sayName("Mercy"));


        for (DemoService demoService1 : demoServicesMap.values()) {

            Assertions.assertEquals(myDemoService, demoService1);

            Assertions.assertEquals(expectedResult, demoService1.sayName("Mercy"));
        }
=======
        String demoServiceResult = "Hello,Mercy";
        Assertions.assertEquals(demoServiceResult + directInvokeSuffix, demoServiceImpl.sayName("Mercy"));
        Assertions.assertEquals(demoServiceResult + callSuffix, testBean.getDemoServiceFromAncestor().sayName("Mercy"));
        Assertions.assertEquals(demoServiceResult + callSuffix, testBean.myDemoService.sayName("Mercy"));
        Assertions.assertEquals(demoServiceResult + callSuffix, testBean.getDemoService().sayName("Mercy"));
        Assertions.assertEquals(demoServiceResult + callSuffix, testBean.getDemoServiceFromParent().sayName("Mercy"));

        DemoService myDemoService = context.getBean("myDemoService", DemoService.class);
        Assertions.assertEquals(demoServiceResult + callSuffix, myDemoService.sayName("Mercy"));
>>>>>>> origin/3.2

    }

    @Test
    void testGetInjectedFieldReferenceBeanMap() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = getReferenceAnnotationBeanPostProcessor();

        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
                beanPostProcessor.getInjectedFieldReferenceBeanMap();

<<<<<<< HEAD
        Assertions.assertEquals(8, referenceBeans.size());
=======
        Assertions.assertEquals(4, referenceBeanMap.size());
>>>>>>> origin/3.2

        Map<String, Integer> checkingFieldNames = new HashMap<>();
        checkingFieldNames.put("private org.apache.dubbo.config.spring.api.HelloService org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessorTest$MyConfiguration.helloService", 0);
        checkingFieldNames.put("private org.apache.dubbo.config.spring.api.HelloService org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessorTest.helloService", 0);
        checkingFieldNames.put("private org.apache.dubbo.config.spring.api.HelloService org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessorTest.helloService2", 0);
        checkingFieldNames.put("private org.apache.dubbo.config.spring.api.DemoService org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessorTest$ParentBean.demoServiceFromParent", 0);

<<<<<<< HEAD
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
=======
        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {
            InjectionMetadata.InjectedElement injectedElement = entry.getKey();
            String member = injectedElement.getMember().toString();
            Integer count = checkingFieldNames.get(member);
            Assertions.assertNotNull(count);
            checkingFieldNames.put(member, count + 1);
        }

        for (Map.Entry<String, Integer> entry : checkingFieldNames.entrySet()) {
            Assertions.assertEquals(1, entry.getValue().intValue(), "check field element failed: " + entry.getKey());
        }
    }

    private ReferenceAnnotationBeanPostProcessor getReferenceAnnotationBeanPostProcessor() {
        return DubboBeanUtils.getReferenceAnnotationBeanPostProcessor(context);
>>>>>>> origin/3.2
    }

    @Test
    void testGetInjectedMethodReferenceBeanMap() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = getReferenceAnnotationBeanPostProcessor();

        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> referenceBeanMap =
                beanPostProcessor.getInjectedMethodReferenceBeanMap();

<<<<<<< HEAD
        Assertions.assertEquals(10, referenceBeanMap.size());
=======
        Assertions.assertEquals(3, referenceBeanMap.size());

        Map<String, Integer> checkingMethodNames = new HashMap<>();
        checkingMethodNames.put("setDemoServiceFromAncestor", 0);
        checkingMethodNames.put("setDemoService", 0);
        checkingMethodNames.put("setHelloService2", 0);
>>>>>>> origin/3.2

        for (Map.Entry<InjectionMetadata.InjectedElement, ReferenceBean<?>> entry : referenceBeanMap.entrySet()) {

            InjectionMetadata.InjectedElement injectedElement = entry.getKey();
<<<<<<< HEAD

            Assertions.assertEquals("com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor$AnnotatedFieldElement",
                    injectedElement.getClass().getName());

=======
            java.lang.reflect.Method method = (java.lang.reflect.Method) injectedElement.getMember();
            Integer count = checkingMethodNames.get(method.getName());
            Assertions.assertNotNull(count);
            Assertions.assertEquals(0, count.intValue());
            checkingMethodNames.put(method.getName(), count + 1);
>>>>>>> origin/3.2
        }

        for (Map.Entry<String, Integer> entry : checkingMethodNames.entrySet()) {
            Assertions.assertEquals(1, entry.getValue().intValue(), "check method element failed: " + entry.getKey());
        }
    }

    @Test
    void testReferenceBeansMethodAnnotation() {

        ReferenceBeanManager referenceBeanManager = context.getBean(ReferenceBeanManager.BEAN_NAME,
                ReferenceBeanManager.class);

        Collection<ReferenceBean> referenceBeans = referenceBeanManager.getReferences();

<<<<<<< HEAD
        Assertions.assertEquals(2, referenceBeanMap.size());
=======
        Assertions.assertEquals(4, referenceBeans.size());
>>>>>>> origin/3.2

        for (ReferenceBean referenceBean : referenceBeans) {
            ReferenceConfig referenceConfig = referenceBean.getReferenceConfig();
            Assertions.assertNotNull(referenceConfig);
            Assertions.assertNotNull(referenceConfig.get());
        }

        ReferenceBean helloServiceReferenceBean = referenceBeanManager.getById("helloService");
        Assertions.assertEquals("helloService", helloServiceReferenceBean.getId());
        ReferenceConfig referenceConfig = helloServiceReferenceBean.getReferenceConfig();
        Assertions.assertEquals(1, referenceConfig.getMethods().size());

<<<<<<< HEAD
            Assertions.assertEquals("com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor$AnnotatedMethodElement",
                    injectedElement.getClass().getName());
=======
        ReferenceBean demoServiceFromParentReferenceBean = referenceBeanManager.getById("demoServiceFromParent");
        ReferenceBean demoServiceReferenceBean = referenceBeanManager.getById("demoService");
        Assertions.assertEquals(demoServiceFromParentReferenceBean.getKey(), demoServiceReferenceBean.getKey());
        Assertions.assertEquals(demoServiceFromParentReferenceBean.getReferenceConfig(), demoServiceReferenceBean.getReferenceConfig());
        Assertions.assertSame(demoServiceFromParentReferenceBean, demoServiceReferenceBean);
>>>>>>> origin/3.2

        ReferenceBean helloService2Bean = referenceBeanManager.getById("helloService2");
        Assertions.assertNotNull(helloService2Bean);
        Assertions.assertNotNull(helloService2Bean.getReferenceConfig());
        Assertions.assertEquals("demo_tag", helloService2Bean.getReferenceConfig().getTag());

        Assertions.assertNotNull(referenceBeanManager.getById("myDemoService"));
        Assertions.assertNotNull(referenceBeanManager.getById("helloService2#2"));

    }

    private static class AncestorBean {

        private DemoService demoServiceFromAncestor;

        @Autowired
        private ApplicationContext applicationContext;

        public DemoService getDemoServiceFromAncestor() {
            return demoServiceFromAncestor;
        }

        // #4 ReferenceBean (Method Injection #2)
        @Reference(id = "myDemoService", version = "2.5.7", url = "dubbo://127.0.0.1:12345?version=2.5.7")
        public void setDemoServiceFromAncestor(DemoService demoServiceFromAncestor) {
            this.demoServiceFromAncestor = demoServiceFromAncestor;
        }

        public ApplicationContext getApplicationContext() {
            return applicationContext;
        }

    }

    private static class ParentBean extends AncestorBean {

        // #2 ReferenceBean (Field Injection #2)
        @Reference(version = "${consumer.version}", url = "${consumer.url}")
        private DemoService demoServiceFromParent;

        public DemoService getDemoServiceFromParent() {
            return demoServiceFromParent;
        }

    }

    static class TestBean extends ParentBean {

        private DemoService demoService;

        @Autowired
        private DemoService myDemoService;

        @Autowired
        private ApplicationContext applicationContext;

        public DemoService getDemoService() {
            return demoService;
        }

        // #3 ReferenceBean (Method Injection #1)
        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345?version=2.5.7")
        public void setDemoService(DemoService demoService) {
            this.demoService = demoService;
        }
    }

<<<<<<< HEAD
    @Test
    public void testReferenceBeansMethodAnnotation() {

        ReferenceAnnotationBeanPostProcessor beanPostProcessor = context.getBean(BEAN_NAME,
                ReferenceAnnotationBeanPostProcessor.class);

        Collection<ReferenceBean<?>> referenceBeans = beanPostProcessor.getReferenceBeans();

        Assertions.assertEquals(8, referenceBeans.size());
=======
    @Configuration
    static class MyConfiguration {
>>>>>>> origin/3.2

        // #1 ReferenceBean (Field Injection #1)
        @Reference(methods = @Method(name = "sayHello", timeout = 100))
        private HelloService helloService;

<<<<<<< HEAD
        if ("helloService".equals(referenceBean.getId())) {
            Assertions.assertNotNull(referenceBean.getMethods());
=======
        @Bean
        public TestBean testBean() {
            return new TestBean();
>>>>>>> origin/3.2
        }

    }
}