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
package org.apache.dubbo.config.spring;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.action.DemoActionByAnnotation;
import org.apache.dubbo.config.spring.action.DemoActionBySetter;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.apache.dubbo.config.spring.filter.MockFilter;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.NotifyService;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.dubbo.common.constants.CommonConstants.SystemProperty.SYSTEM_TCP_RESPONSE_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigTest {

    private static String resourcePath = ConfigTest.class.getPackage().getName().replace('.', '/');

    @BeforeEach
    public void setUp() {
        SysProps.clear();
        DubboBootstrap.reset();
        SysProps.setProperty("dubbo.metrics.enabled", "false");
        SysProps.setProperty("dubbo.metrics.protocol", "disabled");
    }

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testSpringExtensionInject() {
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/spring-extension-inject.xml");
        try {
            ctx.start();
            MockFilter filter = (MockFilter)
                    ExtensionLoader.getExtensionLoader(Filter.class).getExtension("mymock");
            assertNotNull(filter.getMockDao());
            assertNotNull(filter.getProtocol());
            assertNotNull(filter.getLoadBalance());
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    void testServiceClass() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/service-class.xml");
        try {
            ctx.start();

            DemoService demoService = refer("dubbo://127.0.0.1:20887");
            String hello = demoService.sayName("hello");
            assertEquals("welcome:hello", hello);
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testServiceAnnotation() {
        DubboBootstrap consumerBootstrap = null;
        AnnotationConfigApplicationContext providerContext = new AnnotationConfigApplicationContext();
        try {
            providerContext.register(ProviderConfiguration.class);
            providerContext.refresh();

            ReferenceConfig<HelloService> reference = new ReferenceConfig<HelloService>();
            reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
            reference.setInterface(HelloService.class);
            reference.setUrl("dubbo://127.0.0.1:12345");

            consumerBootstrap = DubboBootstrap.newInstance()
                    .application(new ApplicationConfig("consumer"))
                    .reference(reference)
                    .start();
            HelloService helloService = consumerBootstrap.getCache().get(reference);

            String hello = helloService.sayHello("hello");
            assertEquals("Hello, hello", hello);
        } finally {
            providerContext.close();
            if (consumerBootstrap != null) {
                consumerBootstrap.stop();
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProviderNestedService() {
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/provider-nested-service.xml");
        try {
            ctx.start();
            ServiceConfig<DemoService> serviceConfig = (ServiceConfig<DemoService>) ctx.getBean("serviceConfig");
            assertNotNull(serviceConfig.getProvider());
            assertEquals(2000, serviceConfig.getProvider().getTimeout().intValue());

            ServiceConfig<DemoService> serviceConfig2 = (ServiceConfig<DemoService>) ctx.getBean("serviceConfig2");
            assertNotNull(serviceConfig2.getProvider());
            assertEquals(1000, serviceConfig2.getProvider().getTimeout().intValue());
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    private DemoService refer(String url) {
        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(DemoService.class);
        reference.setUrl(url);

        DubboBootstrap bootstrap = DubboBootstrap.newInstance()
                .application(new ApplicationConfig("consumer"))
                .reference(reference)
                .start();
        return bootstrap.getCache().get(reference);
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testToString() {
        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setApplication(new ApplicationConfig("consumer"));
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(DemoService.class);
        reference.setUrl("dubbo://127.0.0.1:20881");
        String str = reference.toString();
        assertTrue(str.startsWith("<dubbo:reference "));
        assertTrue(str.contains(" url=\"dubbo://127.0.0.1:20881\" "));
        assertTrue(str.contains(" interface=\"org.apache.dubbo.config.spring.api.DemoService\" "));
        assertTrue(str.endsWith(" />"));
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testForks() {
        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setApplication(new ApplicationConfig("consumer"));
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(DemoService.class);
        reference.setUrl("dubbo://127.0.0.1:20881");

        int forks = 10;
        reference.setForks(forks);
        String str = reference.toString();
        assertTrue(str.contains("forks=\"" + forks + "\""));
    }

    @Test
    void testMultiProtocol() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol.xml");

        try {
            ctx.start();

            DemoService demoService = refer("dubbo://127.0.0.1:20881");
            String hello = demoService.sayName("hello");
            assertEquals("say:hello", hello);
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testMultiProtocolDefault() {
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol-default.xml");
        try {
            ctx.start();
            DemoService demoService = refer("rmi://127.0.0.1:10991");
            String hello = demoService.sayName("hello");
            assertEquals("say:hello", hello);
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testMultiProtocolError() {
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol-error.xml");
        try {
            ctx.start();
            ctx.stop();
            ctx.close();
            fail();
        } catch (BeanCreationException e) {
            assertTrue(e.getMessage().contains("Found multi-protocols"));
        } finally {
            try {
                ctx.close();
            } catch (Exception e) {
            }
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testMultiProtocolRegister() {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4547, registryService);
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol-register.xml");
        try {
            ctx.start();
            List<URL> urls = registryService.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals(
                    "dubbo://" + NetUtils.getLocalHost() + ":20824/org.apache.dubbo.config.spring.api.DemoService",
                    urls.get(0).toIdentityString());
        } finally {
            ctx.stop();
            ctx.close();
            exporter.unexport();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testMultiRegistry() {
        SimpleRegistryService registryService1 = new SimpleRegistryService();
        Exporter<RegistryService> exporter1 = SimpleRegistryExporter.export(4545, registryService1);
        SimpleRegistryService registryService2 = new SimpleRegistryService();
        Exporter<RegistryService> exporter2 = SimpleRegistryExporter.export(4546, registryService2);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-registry.xml");
        try {
            ctx.start();
            List<URL> urls1 = registryService1.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNull(urls1);
            List<URL> urls2 = registryService2.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNotNull(urls2);
            assertEquals(1, urls2.size());
            assertEquals(
                    "dubbo://" + NetUtils.getLocalHost() + ":20880/org.apache.dubbo.config.spring.api.DemoService",
                    urls2.get(0).toIdentityString());
        } finally {
            ctx.stop();
            ctx.close();
            exporter1.unexport();
            exporter2.unexport();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testDelayFixedTime() throws Exception {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4548, registryService);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/delay-fixed-time.xml");
        try {
            ctx.start();
            List<URL> urls = registryService.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNull(urls);
            int i = 0;
            while ((i++) < 60 && urls == null) {
                urls = registryService.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
                Thread.sleep(10);
            }
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals(
                    "dubbo://" + NetUtils.getLocalHost() + ":20888/org.apache.dubbo.config.spring.api.DemoService",
                    urls.get(0).toIdentityString());
        } finally {
            ctx.stop();
            ctx.close();
            exporter.unexport();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testDelayOnInitialized() throws Exception {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4548, registryService);
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/delay-on-initialized.xml");
        try {
            // ctx.start();
            List<URL> urls = registryService.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals(
                    "dubbo://" + NetUtils.getLocalHost() + ":20888/org.apache.dubbo.config.spring.api.DemoService",
                    urls.get(0).toIdentityString());
        } finally {
            ctx.stop();
            ctx.close();
            exporter.unexport();
        }
    }

    @Test
    void testRmiTimeout() throws Exception {
        SystemPropertyConfigUtils.clearSystemProperty(SYSTEM_TCP_RESPONSE_TIMEOUT);
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setTimeout(1000);
        assertEquals("1000", SystemPropertyConfigUtils.getSystemProperty(SYSTEM_TCP_RESPONSE_TIMEOUT));
        consumer.setTimeout(2000);
        assertEquals("1000", SystemPropertyConfigUtils.getSystemProperty(SYSTEM_TCP_RESPONSE_TIMEOUT));
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testAutowireAndAOP() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml", resourcePath + "/demo-provider-properties.xml");
        try {
            providerContext.start();
            ClassPathXmlApplicationContext byNameContext =
                    new ClassPathXmlApplicationContext(resourcePath + "/aop-autowire-byname.xml");
            try {
                byNameContext.start();
                DemoActionBySetter demoActionBySetter =
                        (DemoActionBySetter) byNameContext.getBean("demoActionBySetter");
                assertNotNull(demoActionBySetter.getDemoService());
                assertEquals(
                        "aop:say:hello", demoActionBySetter.getDemoService().sayName("hello"));
                DemoActionByAnnotation demoActionByAnnotation =
                        (DemoActionByAnnotation) byNameContext.getBean("demoActionByAnnotation");
                assertNotNull(demoActionByAnnotation.getDemoService());
                assertEquals(
                        "aop:say:hello", demoActionByAnnotation.getDemoService().sayName("hello"));
            } finally {
                byNameContext.stop();
                byNameContext.close();
            }
            ClassPathXmlApplicationContext byTypeContext =
                    new ClassPathXmlApplicationContext(resourcePath + "/aop-autowire-bytype.xml");
            try {
                byTypeContext.start();
                DemoActionBySetter demoActionBySetter =
                        (DemoActionBySetter) byTypeContext.getBean("demoActionBySetter");
                assertNotNull(demoActionBySetter.getDemoService());
                assertEquals(
                        "aop:say:hello", demoActionBySetter.getDemoService().sayName("hello"));
                DemoActionByAnnotation demoActionByAnnotation =
                        (DemoActionByAnnotation) byTypeContext.getBean("demoActionByAnnotation");
                assertNotNull(demoActionByAnnotation.getDemoService());
                assertEquals(
                        "aop:say:hello", demoActionByAnnotation.getDemoService().sayName("hello"));
            } finally {
                byTypeContext.stop();
                byTypeContext.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    void testAppendFilter() throws Exception {
        ApplicationConfig application = new ApplicationConfig("provider");

        ProviderConfig provider = new ProviderConfig();
        provider.setFilter("classloader,monitor");

        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setFilter("classloader,monitor");

        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setFilter("accesslog,trace");
        service.setProvider(provider);
        service.setProtocol(new ProtocolConfig("dubbo", 20880));
        service.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setFilter("accesslog,trace");
        reference.setConsumer(consumer);
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(DemoService.class);
        reference.setUrl(
                "dubbo://" + NetUtils.getLocalHost() + ":20880?" + DemoService.class.getName() + "?check=false");

        try {
            DubboBootstrap.getInstance()
                    .application(application)
                    .provider(provider)
                    .service(service)
                    .reference(reference)
                    .start();

            List<URL> urls = service.getExportedUrls();
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals("classloader,monitor,accesslog,trace", urls.get(0).getParameter("service.filter"));

            urls = reference.getExportedUrls();
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals("classloader,monitor,accesslog,trace", urls.get(0).getParameter("reference.filter"));

        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testInitReference() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml", resourcePath + "/demo-provider-properties.xml");

        try {
            providerContext.start();

            // consumer app
            ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(
                    resourcePath + "/init-reference.xml", resourcePath + "/init-reference-properties.xml");
            try {
                consumerContext.start();

                NotifyService notifyService = consumerContext.getBean(NotifyService.class);

                // check reference bean
                Map<String, ReferenceBean> referenceBeanMap = consumerContext.getBeansOfType(ReferenceBean.class);
                Assertions.assertEquals(2, referenceBeanMap.size());
                ReferenceBean referenceBean = referenceBeanMap.get("&demoService");
                Assertions.assertNotNull(referenceBean);
                ReferenceConfig referenceConfig = referenceBean.getReferenceConfig();
                // reference parameters
                Assertions.assertNotNull(referenceConfig.getParameters().get("connec.timeout"));
                Assertions.assertEquals("demo_tag", referenceConfig.getTag());

                // methods
                Assertions.assertEquals(1, referenceConfig.getMethods().size());
                MethodConfig methodConfig = referenceConfig.getMethods().get(0);
                Assertions.assertEquals("sayName", methodConfig.getName());
                Assertions.assertEquals(notifyService, methodConfig.getOninvoke());
                Assertions.assertEquals(notifyService, methodConfig.getOnreturn());
                Assertions.assertEquals(notifyService, methodConfig.getOnthrow());
                Assertions.assertEquals("onInvoke", methodConfig.getOninvokeMethod());
                Assertions.assertEquals("onReturn", methodConfig.getOnreturnMethod());
                Assertions.assertEquals("onThrow", methodConfig.getOnthrowMethod());

                // method arguments
                Assertions.assertEquals(1, methodConfig.getArguments().size());
                ArgumentConfig argumentConfig = methodConfig.getArguments().get(0);
                Assertions.assertEquals(0, argumentConfig.getIndex());
                Assertions.assertEquals(true, argumentConfig.isCallback());

                // method parameters
                Assertions.assertEquals(1, methodConfig.getParameters().size());
                Assertions.assertEquals("my-token", methodConfig.getParameters().get("access-token"));

                // do call
                DemoService demoService = (DemoService) consumerContext.getBean("demoService");
                assertEquals("say:world", demoService.sayName("world"));

                GenericService demoService2 = (GenericService) consumerContext.getBean("demoService2");
                assertEquals(
                        "say:world",
                        demoService2.$invoke("sayName", new String[] {"java.lang.String"}, new Object[] {"world"}));

            } finally {
                consumerContext.stop();
                consumerContext.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    void test_noMethodInterface_methodsKeyHasValue() throws Exception {
        List<URL> urls = null;
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(resourcePath + "/demo-provider-no-methods-interface.xml");
        try {
            ctx.start();

            ServiceBean bean = (ServiceBean) ctx.getBean("service");
            urls = bean.getExportedUrls();
            assertEquals(1, urls.size());
            URL url = urls.get(0);
            assertEquals("getBox,sayName", url.getParameter("methods"));
        } finally {
            ctx.stop();
            ctx.close();
            // Check if the port is closed
            if (urls != null) {
                for (URL url : urls) {
                    Assertions.assertFalse(NetUtils.isPortInUsed(url.getPort()));
                }
            }
        }
    }
}
