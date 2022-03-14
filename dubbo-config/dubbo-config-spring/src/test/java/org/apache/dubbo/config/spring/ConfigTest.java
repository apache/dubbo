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
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.spring.action.DemoActionByAnnotation;
import org.apache.dubbo.config.spring.action.DemoActionBySetter;
import org.apache.dubbo.config.spring.annotation.consumer.AnnotationAction;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.apache.dubbo.config.spring.filter.MockFilter;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.config.spring.impl.NotifyService;
import org.apache.dubbo.config.spring.registry.MockRegistry;
import org.apache.dubbo.config.spring.registry.MockRegistryFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_BEAN;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * ConfigTest
 */
public class ConfigTest {

    private static String resourcePath = ConfigTest.class.getPackage().getName().replace('.', '/');

    @BeforeEach
    public void setUp() {
        SysProps.clear();
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
        SysProps.clear();
        DubboBootstrap.reset();
    }


    @Test
    @Disabled("waiting-to-fix")
    public void testSpringExtensionInject() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/spring-extension-inject.xml");
        try {
            ctx.start();
            MockFilter filter = (MockFilter) ExtensionLoader.getExtensionLoader(Filter.class).getExtension("mymock");
            assertNotNull(filter.getMockDao());
            assertNotNull(filter.getProtocol());
            assertNotNull(filter.getLoadBalance());
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    public void testServiceClass() {
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/provider-nested-service.xml");
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
    public void testMultiProtocol() {
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol-default.xml");
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol-error.xml");
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol-register.xml");
        try {
            ctx.start();
            List<URL> urls = registryService.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20824/org.apache.dubbo.config.spring.api.DemoService", urls.get(0).toIdentityString());
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
            assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20880/org.apache.dubbo.config.spring.api.DemoService", urls2.get(0).toIdentityString());
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
            assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20888/org.apache.dubbo.config.spring.api.DemoService", urls.get(0).toIdentityString());
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/delay-on-initialized.xml");
        try {
            //ctx.start();
            List<URL> urls = registryService.getRegistered().get("org.apache.dubbo.config.spring.api.DemoService");
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals("dubbo://" + NetUtils.getLocalHost() + ":20888/org.apache.dubbo.config.spring.api.DemoService", urls.get(0).toIdentityString());
        } finally {
            ctx.stop();
            ctx.close();
            exporter.unexport();
        }
    }

    @Test
    public void testRmiTimeout() throws Exception {
        System.clearProperty("sun.rmi.transport.tcp.responseTimeout");
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setTimeout(1000);
        assertEquals("1000", System.getProperty("sun.rmi.transport.tcp.responseTimeout"));
        consumer.setTimeout(2000);
        assertEquals("1000", System.getProperty("sun.rmi.transport.tcp.responseTimeout"));
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testAutowireAndAOP() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml",
                resourcePath + "/demo-provider-properties.xml");
        try {
            providerContext.start();
            ClassPathXmlApplicationContext byNameContext = new ClassPathXmlApplicationContext(resourcePath + "/aop-autowire-byname.xml");
            try {
                byNameContext.start();
                DemoActionBySetter demoActionBySetter = (DemoActionBySetter) byNameContext.getBean("demoActionBySetter");
                assertNotNull(demoActionBySetter.getDemoService());
                assertEquals("aop:say:hello", demoActionBySetter.getDemoService().sayName("hello"));
                DemoActionByAnnotation demoActionByAnnotation = (DemoActionByAnnotation) byNameContext.getBean("demoActionByAnnotation");
                assertNotNull(demoActionByAnnotation.getDemoService());
                assertEquals("aop:say:hello", demoActionByAnnotation.getDemoService().sayName("hello"));
            } finally {
                byNameContext.stop();
                byNameContext.close();
            }
            ClassPathXmlApplicationContext byTypeContext = new ClassPathXmlApplicationContext(resourcePath + "/aop-autowire-bytype.xml");
            try {
                byTypeContext.start();
                DemoActionBySetter demoActionBySetter = (DemoActionBySetter) byTypeContext.getBean("demoActionBySetter");
                assertNotNull(demoActionBySetter.getDemoService());
                assertEquals("aop:say:hello", demoActionBySetter.getDemoService().sayName("hello"));
                DemoActionByAnnotation demoActionByAnnotation = (DemoActionByAnnotation) byTypeContext.getBean("demoActionByAnnotation");
                assertNotNull(demoActionByAnnotation.getDemoService());
                assertEquals("aop:say:hello", demoActionByAnnotation.getDemoService().sayName("hello"));
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
    public void testAppendFilter() throws Exception {
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
        reference.setUrl("dubbo://" + NetUtils.getLocalHost() + ":20880?" + DemoService.class.getName() + "?check=false");


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
    public void testInitReference() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml",
                resourcePath + "/demo-provider-properties.xml");

        try {
            providerContext.start();

            // consumer app
            ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(resourcePath + "/init-reference.xml",
                    resourcePath + "/init-reference-properties.xml");
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

                //methods
                Assertions.assertEquals(1, referenceConfig.getMethods().size());
                MethodConfig methodConfig = referenceConfig.getMethods().get(0);
                Assertions.assertEquals("sayName", methodConfig.getName());
                Assertions.assertEquals(notifyService, methodConfig.getOninvoke());
                Assertions.assertEquals(notifyService, methodConfig.getOnreturn());
                Assertions.assertEquals(notifyService, methodConfig.getOnthrow());
                Assertions.assertEquals("onInvoke", methodConfig.getOninvokeMethod());
                Assertions.assertEquals("onReturn", methodConfig.getOnreturnMethod());
                Assertions.assertEquals("onThrow", methodConfig.getOnthrowMethod());

                //method arguments
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
                assertEquals("say:world", demoService2.$invoke("sayName", new String[]{"java.lang.String"}, new Object[]{"world"}));

            } finally {
                consumerContext.stop();
                consumerContext.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    // DUBBO-571 methods key in provider's URLONE doesn't contain the methods from inherited super interface
    @Test
    public void test_noMethodInterface_methodsKeyHasValue() throws Exception {
        List<URL> urls = null;
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/demo-provider-no-methods-interface.xml");
        try {
            ctx.start();

            ServiceBean bean = (ServiceBean) ctx.getBean("service");
            urls = bean.getExportedUrls();
            assertEquals(1, urls.size());
            URL url = urls.get(0);
            assertEquals("sayName,getBox", url.getParameter("methods"));
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

    // DUBBO-147 find all invoker instances which have been tried from RpcContext
    @Disabled("waiting-to-fix")
    @Test
    public void test_RpcContext_getUrls() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider-long-waiting.xml");

        try {
            providerContext.start();

            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/init-reference-getUrls.xml");
            try {
                ctx.start();
                DemoService demoService = (DemoService) ctx.getBean("demoService");
                try {
                    demoService.sayName("Haha");
                    fail();
                } catch (RpcException expected) {
                    assertThat(expected.getMessage(), containsString("Tried 3 times"));
                }

                assertEquals(3, RpcContext.getServiceContext().getUrls().size());
            } finally {
                ctx.stop();
                ctx.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    // BUG: DUBBO-846 in version 2.0.9, config retry="false" on provider's method doesn't work
    @Test
    @Disabled("waiting-to-fix")
    public void test_retrySettingFail() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/demo-provider-long-waiting.xml");

        try {
            providerContext.start();
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                    resourcePath + "/init-reference-retry-false.xml");
            try {
                ctx.start();
                DemoService demoService = (DemoService) ctx.getBean("demoService");
                try {
                    demoService.sayName("Haha");
                    fail();
                } catch (RpcException expected) {
                    assertThat(expected.getMessage(), containsString("Tried 1 times"));
                }

                assertEquals(1, RpcContext.getServiceContext().getUrls().size());
            } finally {
                ctx.stop();
                ctx.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    // BuG: DUBBO-146 Provider doesn't have exception output, and consumer has timeout error when serialization fails
    // for example, object transported on the wire doesn't implement Serializable
    @Test
    @Disabled("waiting-to-fix")
    public void test_returnSerializationFail() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/demo-provider-UnserializableBox.xml");
        try {
            providerContext.start();
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/init-reference.xml",
                    resourcePath + "/init-reference-properties.xml");
            try {
                ctx.start();
                DemoService demoService = (DemoService) ctx.getBean("demoService");
                try {
                    demoService.getBox();
                    fail();
                } catch (RpcException expected) {
                    assertThat(expected.getMessage(), containsString("must implement java.io.Serializable"));
                }
            } finally {
                ctx.stop();
                ctx.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testXmlOverrideProperties() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/xml-override-properties.xml");
        try {
            providerContext.start();
            ApplicationConfig application = (ApplicationConfig) providerContext.getBean("application");
            assertEquals("demo-provider", application.getName());
            assertEquals("world", application.getOwner());

            RegistryConfig registry = (RegistryConfig) providerContext.getBean("registry");
            assertEquals("N/A", registry.getAddress());

            ProtocolConfig dubbo = (ProtocolConfig) providerContext.getBean("dubbo");
            assertEquals(20813, dubbo.getPort().intValue());

        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testApiOverrideProperties() throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("api-override-properties");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("N/A");

        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(13123);

        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setRegistry(registry);
        service.setProtocol(protocol);

        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(DemoService.class);
        reference.setUrl("dubbo://127.0.0.1:13123");

        try {
            DubboBootstrap.getInstance()
                .application(application)
                .registry(registry)
                .protocol(protocol)
                .service(service)
                .reference(reference)
                .start();

            URL url = service.getExportedUrls().get(0);
            assertEquals("api-override-properties", url.getParameter("application"));
            assertEquals("world", url.getParameter("owner"));
            assertEquals(13123, url.getPort());

            url = reference.getExportedUrls().get(0);
            assertEquals("2000", url.getParameter("timeout"));
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    public void testSystemPropertyOverrideProtocol() throws Exception {
        SysProps.setProperty("dubbo.protocols.tri.port", ""); // empty config should be ignored
        SysProps.setProperty("dubbo.protocols.dubbo.port", "20812"); // override success
        SysProps.setProperty("dubbo.protocol.port", "20899"); // override fail
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/override-protocol.xml");
        try {
            providerContext.start();
            ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
            ProtocolConfig protocol = configManager.getProtocol("dubbo").get();
            assertEquals(20812, protocol.getPort());
        } finally {
            providerContext.close();
        }
    }

    @Test
    public void testSystemPropertyOverrideMultiProtocol() throws Exception {
        SysProps.setProperty("dubbo.protocols.dubbo.port", "20814");
        SysProps.setProperty("dubbo.protocols.tri.port", "10914");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath +
                "/override-multi-protocol.xml");
        try {
            providerContext.start();
            ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();

            ProtocolConfig dubboProtocol = configManager.getProtocol("dubbo").get();
            assertEquals(20814, dubboProtocol.getPort().intValue());
            ProtocolConfig tripleProtocol = configManager.getProtocol("tri").get();
            assertEquals(10914, tripleProtocol.getPort().intValue());
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    @Disabled("waiting-to-fix")
    public void testSystemPropertyOverrideXmlDefault() throws Exception {
        SysProps.setProperty("dubbo.application.name", "sysover");
        SysProps.setProperty("dubbo.application.owner", "sysowner");
        SysProps.setProperty("dubbo.registry.address", "N/A");
        SysProps.setProperty("dubbo.protocol.name", "dubbo");
        SysProps.setProperty("dubbo.protocol.port", "20819");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/system-properties-override-default.xml");
        try {
            providerContext.start();
            ServiceConfig<DemoService> service = (ServiceConfig<DemoService>) providerContext.getBean("demoServiceConfig");
            assertEquals("sysover", service.getApplication().getName());
            assertEquals("sysowner", service.getApplication().getOwner());
            assertEquals("N/A", service.getRegistry().getAddress());
            assertEquals("dubbo", service.getProtocol().getName());
            assertEquals(20819, service.getProtocol().getPort().intValue());
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    @Disabled("waiting-to-fix")
    public void testSystemPropertyOverrideXml() throws Exception {
        SysProps.setProperty("dubbo.application.name", "sysover");
        SysProps.setProperty("dubbo.application.owner", "sysowner");
        SysProps.setProperty("dubbo.registry.address", "N/A");
        SysProps.setProperty("dubbo.protocol.name", "dubbo");
        SysProps.setProperty("dubbo.protocol.port", "20819");
        SysProps.setProperty("dubbo.service.register", "false");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/system-properties-override.xml");
        try {
            providerContext.start();
            ServiceConfig<DemoService> service = (ServiceConfig<DemoService>) providerContext.getBean("demoServiceConfig");
            URL url = service.getExportedUrls().get(0);
            assertEquals("sysover", url.getParameter("application"));
            assertEquals("sysowner", url.getParameter("owner"));
            assertEquals("dubbo", url.getProtocol());
            assertEquals(20819, url.getPort());
            String register = url.getParameter("register");
            assertTrue(register != null && !"".equals(register));
            assertEquals(false, Boolean.valueOf(register));
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    public void testSystemPropertyOverrideReferenceConfig() throws Exception {
        SysProps.setProperty("dubbo.reference.org.apache.dubbo.config.spring.api.DemoService.retries", "5");
        SysProps.setProperty("dubbo.consumer.check", "false");
        SysProps.setProperty("dubbo.consumer.timeout", "1234");

        try {
            ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            ProtocolConfig protocolConfig = new ProtocolConfig("injvm");

            ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
            reference.setInterface(DemoService.class);
            reference.setInjvm(true);
            reference.setRetries(2);

            DubboBootstrap.getInstance()
                    .application(new ApplicationConfig("testSystemPropertyOverrideReferenceConfig"))
                    .registry(new RegistryConfig(RegistryConfig.NO_AVAILABLE))
                    .protocol(protocolConfig)
                    .service(service)
                    .reference(reference)
                    .start();

            // override retries
            assertEquals(Integer.valueOf(5), reference.getRetries());
            // set default value of check
            assertEquals(false, reference.shouldCheck());

            ModuleConfigManager moduleConfigManager = ApplicationModel.defaultModel().getDefaultModule().getConfigManager();
            ConsumerConfig defaultConsumer = moduleConfigManager.getDefaultConsumer().get();
            assertEquals(1234, defaultConsumer.getTimeout());
            assertEquals(false, defaultConsumer.isCheck());
        } finally {
            // If we don't stop here, somewhere else will throw BeanCreationException of duplication.
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testSystemPropertyOverrideApiDefault() throws Exception {
        SysProps.setProperty("dubbo.application.name", "sysover");
        SysProps.setProperty("dubbo.application.owner", "sysowner");
        SysProps.setProperty("dubbo.registry.address", "N/A");
        SysProps.setProperty("dubbo.protocol.name", "dubbo");
        SysProps.setProperty("dubbo.protocol.port", "20834");

        try {
            ServiceConfig<DemoService> serviceConfig = new ServiceConfig<DemoService>();
            serviceConfig.setInterface(DemoService.class);
            serviceConfig.setRef(new DemoServiceImpl());

            DubboBootstrap.getInstance()
                .service(serviceConfig)
                .start();

            assertEquals("sysover", serviceConfig.getApplication().getName());
            assertEquals("sysowner", serviceConfig.getApplication().getOwner());
            assertEquals("N/A", serviceConfig.getRegistry().getAddress());
            assertEquals("dubbo", serviceConfig.getProtocol().getName());
            assertEquals(20834, serviceConfig.getProtocol().getPort().intValue());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testSystemPropertyOverrideApi() throws Exception {
        SysProps.setProperty("dubbo.application.name", "sysover");
        SysProps.setProperty("dubbo.application.owner", "sysowner");
        SysProps.setProperty("dubbo.registry.address", "N/A");
        SysProps.setProperty("dubbo.protocol.name", "dubbo");
        SysProps.setProperty("dubbo.protocol.port", "20834");
        try {
            ApplicationConfig application = new ApplicationConfig();
            application.setName("aaa");

            RegistryConfig registry = new RegistryConfig();
            registry.setAddress("127.0.0.1");

            ProtocolConfig protocol = new ProtocolConfig();
            protocol.setName("rmi");
            protocol.setPort(1099);

            ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setApplication(application);
            service.setRegistry(registry);
            service.setProtocol(protocol);

            DubboBootstrap.getInstance()
                    .application(application)
                    .registry(registry)
                    .protocol(protocol)
                    .service(service)
                    .start();

            URL url = service.getExportedUrls().get(0);
            assertEquals("sysover", url.getParameter("application"));
            assertEquals("sysowner", url.getParameter("owner"));
            assertEquals("dubbo", url.getProtocol());
            assertEquals(20834, url.getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testSystemPropertyOverrideProperties() throws Exception {
        try {
            int port = 1234;
            SysProps.setProperty("dubbo.protocol.port", String.valueOf(port));
            ApplicationConfig application = new ApplicationConfig();
            application.setName("aaa");

            RegistryConfig registry = new RegistryConfig();
            registry.setAddress("N/A");

            ProtocolConfig protocol = new ProtocolConfig();
            protocol.setName("rmi");

            ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setApplication(application);
            service.setRegistry(registry);
            service.setProtocol(protocol);

            DubboBootstrap.getInstance()
                    .application(application)
                    .registry(registry)
                    .protocol(protocol)
                    .service(service)
                    .start();

            URL url = service.getExportedUrls().get(0);
            // from api
            assertEquals("aaa", url.getParameter("application"));
            // from dubbo-binder.properties
            assertEquals("world", url.getParameter("owner"));
            // from system property
            assertEquals(1234, url.getPort());
        } finally {
            System.clearProperty("dubbo.protocol.port");
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    @SuppressWarnings("unchecked")
    public void testCustomizeParameter() throws Exception {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(resourcePath + "/customize-parameter.xml");
        try {
            context.start();
            ServiceBean<DemoService> serviceBean = (ServiceBean<DemoService>) context.getBean("demoServiceExport");
            URL url = (URL) serviceBean.getExportedUrls().get(0);
            assertEquals("protocol-paramA", url.getParameter("protocol.paramA"));
            assertEquals("service-paramA", url.getParameter("service.paramA"));
        } finally {
            context.close();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testPath() throws Exception {
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setPath("a/b$c");
        try {
            service.setPath("a?b");
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(""));
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testAnnotation() {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4548, registryService);
        try {
            SysProps.setProperty("provider.version", "1.2");
            ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(resourcePath + "/annotation-provider.xml");
            try {
                providerContext.start();

                ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(resourcePath + "/annotation-consumer.xml");
                try {
                    consumerContext.start();
                    AnnotationAction annotationAction = (AnnotationAction) consumerContext.getBean("annotationAction");
                    String hello = annotationAction.doSayName("hello");
                    assertEquals("annotation:hello", hello);
                } finally {
                    consumerContext.stop();
                    consumerContext.close();
                }
            } finally {
                providerContext.stop();
                providerContext.close();
            }
        } finally {
            System.clearProperty("provider.version");
            exporter.unexport();
        }
    }

    @Test
    public void testDubboProtocolPortOverride() throws Exception {
        int port = NetUtils.getAvailablePort();
        SysProps.setProperty("dubbo.protocol.port", String.valueOf(port));
        ServiceConfig<DemoService> service = null;
        try {
            ApplicationConfig application = new ApplicationConfig();
            application.setName("dubbo-protocol-port-override");

            RegistryConfig registry = new RegistryConfig();
            registry.setAddress("N/A");

            ProtocolConfig protocol = new ProtocolConfig();

            service = new ServiceConfig<DemoService>();
            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setApplication(application);
            service.setRegistry(registry);
            service.setProtocol(protocol);


            DubboBootstrap.getInstance()
                    .application(application)
                    .registry(registry)
                    .protocol(protocol)
                    .service(service)
                    .start();

            assertEquals(port, service.getExportedUrls().get(0).getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    @Disabled("waiting-to-fix")
    public void testProtocolRandomPort() throws Exception {
        ServiceConfig<DemoService> demoService = null;
        ServiceConfig<HelloService> helloService = null;

        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-protocol-random-port");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("N/A");

        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(-1);

        demoService = new ServiceConfig<DemoService>();
        demoService.setInterface(DemoService.class);
        demoService.setRef(new DemoServiceImpl());
        demoService.setApplication(application);
        demoService.setRegistry(registry);
        demoService.setProtocol(protocol);

        helloService = new ServiceConfig<HelloService>();
        helloService.setInterface(HelloService.class);
        helloService.setRef(new HelloServiceImpl());
        helloService.setApplication(application);
        helloService.setRegistry(registry);
        helloService.setProtocol(protocol);

        try {
            DubboBootstrap.getInstance()
                .application(application)
                .registry(registry)
                .protocol(protocol)
                .service(demoService)
                .service(helloService)
                .start();

            assertEquals(demoService.getExportedUrls().get(0).getPort(),
                    helloService.getExportedUrls().get(0).getPort());
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    @Disabled("waiting-to-fix, see: https://github.com/apache/dubbo/pull/8534")
    public void testReferGenericExport() throws Exception {
        RegistryConfig rc = new RegistryConfig();
        rc.setAddress(RegistryConfig.NO_AVAILABLE);

        ServiceConfig<GenericService> sc = new ServiceConfig<GenericService>();
        sc.setRegistry(rc);
        sc.setInterface(DemoService.class.getName());
        sc.setRef((method, parameterTypes, args) -> null);

        ReferenceConfig<DemoService> ref = new ReferenceConfig<DemoService>();
        ref.setRegistry(rc);
        ref.setInterface(DemoService.class.getName());

        try {
            DubboBootstrap.getInstance()
                .application(new ApplicationConfig("test-refer-generic-export"))
                .service(sc)
                .reference(ref)
                .start();
            fail();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    public void testGenericServiceConfig() throws Exception {
        ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
        service.setRegistry(new RegistryConfig("mock://localhost"));
        service.setInterface(DemoService.class.getName());
        service.setGeneric(GENERIC_SERIALIZATION_BEAN);
        service.setRef((method, parameterTypes, args) -> null);

        try {
            DubboBootstrap.getInstance()
                .application(new ApplicationConfig("test"))
                .service(service)
                .start();

            Collection<Registry> collection = MockRegistryFactory.getCachedRegistry();
            MockRegistry registry = (MockRegistry) collection.iterator().next();
            URL url = registry.getRegistered().get(0);
            assertEquals(GENERIC_SERIALIZATION_BEAN, url.getParameter(GENERIC_KEY));
        } finally {
            MockRegistryFactory.cleanCachedRegistry();
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    public void testGenericServiceConfigThroughSpring() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/generic-export.xml");
        try {
            ctx.start();
            ServiceConfig serviceConfig = (ServiceConfig) ctx.getBean("dubboDemoService");
            URL url = (URL) serviceConfig.getExportedUrls().get(0);
            assertEquals(GENERIC_SERIALIZATION_BEAN, url.getParameter(GENERIC_KEY));
        } finally {
            ctx.close();
        }
    }

}
