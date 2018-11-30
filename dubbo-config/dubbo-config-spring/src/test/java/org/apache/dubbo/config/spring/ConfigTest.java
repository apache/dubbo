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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spring.action.DemoActionByAnnotation;
import org.apache.dubbo.config.spring.action.DemoActionBySetter;
import org.apache.dubbo.config.spring.annotation.consumer.AnnotationAction;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.apache.dubbo.config.spring.filter.MockFilter;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.HelloServiceImpl;
import org.apache.dubbo.config.spring.registry.MockRegistry;
import org.apache.dubbo.config.spring.registry.MockRegistryFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;


/**
 * ConfigTest
 */
@Ignore
public class ConfigTest {

    private static void unexportService(ServiceConfig<?> config) {
        if (config != null) {
            config.unexport();
        }
    }

    @Test
    public void testSpringExtensionInject() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/spring-extension-inject.xml");
        ctx.start();
        try {
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/service-class.xml");
        ctx.start();
        try {
            DemoService demoService = refer("dubbo://127.0.0.1:30887");
            String hello = demoService.sayName("hello");
            assertEquals("welcome:hello", hello);
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    public void testServiceAnnotation() {
        AnnotationConfigApplicationContext providerContext = new AnnotationConfigApplicationContext();
        providerContext.register(ProviderConfiguration.class);

        providerContext.refresh();

        ReferenceConfig<HelloService> reference = new ReferenceConfig<HelloService>();
        reference.setApplication(new ApplicationConfig("consumer"));
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(HelloService.class);
        reference.setUrl("dubbo://127.0.0.1:12345");
        String hello = reference.get().sayHello("hello");
        assertEquals("Hello, hello", hello);

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProviderNestedService() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/provider-nested-service.xml");
        ctx.start();
        try {
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
        reference.setApplication(new ApplicationConfig("consumer"));
        reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        reference.setInterface(DemoService.class);
        reference.setUrl(url);
        return reference.get();
    }

    @Test
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
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-protocol.xml");
        ctx.start();
        try {
            DemoService demoService = refer("dubbo://127.0.0.1:20881");
            String hello = demoService.sayName("hello");
            assertEquals("say:hello", hello);
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    public void testMultiProtocolDefault() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-protocol-default.xml");
        ctx.start();
        try {
            DemoService demoService = refer("rmi://127.0.0.1:10991");
            String hello = demoService.sayName("hello");
            assertEquals("say:hello", hello);
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    @Test
    public void testMultiProtocolError() {
        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-protocol-error.xml");
            ctx.start();
            ctx.stop();
            ctx.close();
        } catch (BeanCreationException e) {
            assertTrue(e.getMessage().contains("Found multi-protocols"));
        }
    }

    @Test
    public void testMultiProtocolRegister() {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4547, registryService);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-protocol-register.xml");
        ctx.start();
        try {
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
    public void testMultiRegistry() {
        SimpleRegistryService registryService1 = new SimpleRegistryService();
        Exporter<RegistryService> exporter1 = SimpleRegistryExporter.export(4545, registryService1);
        SimpleRegistryService registryService2 = new SimpleRegistryService();
        Exporter<RegistryService> exporter2 = SimpleRegistryExporter.export(4546, registryService2);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-registry.xml");
        ctx.start();
        try {
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
    public void testDelayFixedTime() throws Exception {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4548, registryService);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/delay-fixed-time.xml");
        ctx.start();
        try {
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
    public void testDelayOnInitialized() throws Exception {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4548, registryService);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/delay-on-initialized.xml");
        //ctx.start();
        try {
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
        if (System.getProperty("sun.rmi.transport.tcp.responseTimeout") != null) {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", "");
        }
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setTimeout(1000);
        assertEquals("1000", System.getProperty("sun.rmi.transport.tcp.responseTimeout"));
        consumer.setTimeout(2000);
        assertEquals("1000", System.getProperty("sun.rmi.transport.tcp.responseTimeout"));
    }

    @Test
    public void testAutowireAndAOP() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider.xml");
        providerContext.start();
        try {
            ClassPathXmlApplicationContext byNameContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/aop-autowire-byname.xml");
            byNameContext.start();
            try {
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
            ClassPathXmlApplicationContext byTypeContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/aop-autowire-bytype.xml");
            byTypeContext.start();
            try {
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
        ProviderConfig provider = new ProviderConfig();
        provider.setFilter("classloader,monitor");
        ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
        service.setFilter("accesslog,trace");
        service.setProvider(provider);
        service.setProtocol(new ProtocolConfig("dubbo", 20880));
        service.setApplication(new ApplicationConfig("provider"));
        service.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        try {
            service.export();
            List<URL> urls = service.toUrls();
            assertNotNull(urls);
            assertEquals(1, urls.size());
            assertEquals("classloader,monitor,accesslog,trace", urls.get(0).getParameter("service.filter"));

            ConsumerConfig consumer = new ConsumerConfig();
            consumer.setFilter("classloader,monitor");
            ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
            reference.setFilter("accesslog,trace");
            reference.setConsumer(consumer);
            reference.setApplication(new ApplicationConfig("consumer"));
            reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://" + NetUtils.getLocalHost() + ":20880?" + DemoService.class.getName() + "?check=false");
            try {
                reference.get();
                urls = reference.toUrls();
                assertNotNull(urls);
                assertEquals(1, urls.size());
                assertEquals("classloader,monitor,accesslog,trace", urls.get(0).getParameter("reference.filter"));
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

    @Test
    public void testInitReference() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider.xml");
        providerContext.start();
        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/init-reference.xml");
            ctx.start();
            try {
                DemoService demoService = (DemoService) ctx.getBean("demoService");
                assertEquals("say:world", demoService.sayName("world"));
            } finally {
                ctx.stop();
                ctx.close();
            }
        } finally {
            providerContext.stop();
            providerContext.close();
        }
    }

    // DUBBO-571 methods key in provider's URLONE doesn't contain the methods from inherited super interface
    @Test
    public void test_noMethodInterface_methodsKeyHasValue() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider-no-methods-interface.xml");
        ctx.start();
        try {
            ServiceBean bean = (ServiceBean) ctx.getBean("service");
            List<URL> urls = bean.getExportedUrls();
            assertEquals(1, urls.size());
            URL url = urls.get(0);
            assertEquals("sayName,getBox", url.getParameter("methods"));
        } finally {
            ctx.stop();
            ctx.close();
        }
    }

    // DUBBO-147 find all invoker instances which have been tried from RpcContext
    @Test
    public void test_RpcContext_getUrls() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider-long-waiting.xml");
        providerContext.start();

        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                    ConfigTest.class.getPackage().getName().replace('.', '/')
                            + "/init-reference-getUrls.xml");
            ctx.start();
            try {
                DemoService demoService = (DemoService) ctx.getBean("demoService");
                try {
                    demoService.sayName("Haha");
                    fail();
                } catch (RpcException expected) {
                    assertThat(expected.getMessage(), containsString("Tried 3 times"));
                }

                assertEquals(3, RpcContext.getContext().getUrls().size());
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
    public void test_retrySettingFail() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(
                ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider-long-waiting.xml");
        providerContext.start();

        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                    ConfigTest.class.getPackage().getName().replace('.', '/')
                            + "/init-reference-retry-false.xml");
            ctx.start();
            try {
                DemoService demoService = (DemoService) ctx.getBean("demoService");
                try {
                    demoService.sayName("Haha");
                    fail();
                } catch (RpcException expected) {
                    assertThat(expected.getMessage(), containsString("Tried 1 times"));
                }

                assertEquals(1, RpcContext.getContext().getUrls().size());
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
    public void test_returnSerializationFail() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider-UnserializableBox.xml");
        providerContext.start();
        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/init-reference.xml");
            ctx.start();
            try {
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
    public void testXmlOverrideProperties() throws Exception {
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/xml-override-properties.xml");
        providerContext.start();
        try {
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
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.export();

        try {
            URL url = service.toUrls().get(0);
            assertEquals("api-override-properties", url.getParameter("application"));
            assertEquals("world", url.getParameter("owner"));
            assertEquals(13123, url.getPort());

            ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
            reference.setApplication(new ApplicationConfig("consumer"));
            reference.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
            reference.setInterface(DemoService.class);
            reference.setUrl("dubbo://127.0.0.1:13123");
            reference.get();
            try {
                url = reference.toUrls().get(0);
                assertEquals("2000", url.getParameter("timeout"));
            } finally {
                reference.destroy();
            }
        } finally {
            service.unexport();
        }
    }

    @Test
    public void testSystemPropertyOverrideProtocol() throws Exception {
        System.setProperty("dubbo.protocol.port", "20812");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/override-protocol.xml");
        providerContext.start();
        try {
            ProtocolConfig dubbo = (ProtocolConfig) providerContext.getBean("dubbo");
            assertEquals(20812, dubbo.getPort().intValue());
        } finally {
            System.setProperty("dubbo.protocol.port", "");
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    public void testSystemPropertyOverrideMultiProtocol() throws Exception {
        System.setProperty("dubbo.protocol.dubbo.port", "20814");
        System.setProperty("dubbo.protocol.rmi.port", "10914");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/override-multi-protocol.xml");
        providerContext.start();
        try {
            ProtocolConfig dubbo = (ProtocolConfig) providerContext.getBean("dubbo");
            assertEquals(20814, dubbo.getPort().intValue());
            ProtocolConfig rmi = (ProtocolConfig) providerContext.getBean("rmi");
            assertEquals(10914, rmi.getPort().intValue());
        } finally {
            System.setProperty("dubbo.protocol.dubbo.port", "");
            System.setProperty("dubbo.protocol.rmi.port", "");
            providerContext.stop();
            providerContext.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSystemPropertyOverrideXmlDefault() throws Exception {
        System.setProperty("dubbo.application.name", "sysover");
        System.setProperty("dubbo.application.owner", "sysowner");
        System.setProperty("dubbo.registry.address", "N/A");
        System.setProperty("dubbo.protocol.name", "dubbo");
        System.setProperty("dubbo.protocol.port", "20819");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/system-properties-override-default.xml");
        providerContext.start();
        try {
            ServiceConfig<DemoService> service = (ServiceConfig<DemoService>) providerContext.getBean("demoServiceConfig");
            assertEquals("sysover", service.getApplication().getName());
            assertEquals("sysowner", service.getApplication().getOwner());
            assertEquals("N/A", service.getRegistry().getAddress());
            assertEquals("dubbo", service.getProtocol().getName());
            assertEquals(20819, service.getProtocol().getPort().intValue());
        } finally {
            System.setProperty("dubbo.application.name", "");
            System.setProperty("dubbo.application.owner", "");
            System.setProperty("dubbo.registry.address", "");
            System.setProperty("dubbo.protocol.name", "");
            System.setProperty("dubbo.protocol.port", "");
            providerContext.stop();
            providerContext.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSystemPropertyOverrideXml() throws Exception {
        System.setProperty("dubbo.application.name", "sysover");
        System.setProperty("dubbo.application.owner", "sysowner");
        System.setProperty("dubbo.registry.address", "N/A");
        System.setProperty("dubbo.protocol.name", "dubbo");
        System.setProperty("dubbo.protocol.port", "20819");
        System.setProperty("dubbo.service.register", "false");
        ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/system-properties-override.xml");
        providerContext.start();
        try {
            ServiceConfig<DemoService> service = (ServiceConfig<DemoService>) providerContext.getBean("demoServiceConfig");
            URL url = service.toUrls().get(0);
            assertEquals("sysover", url.getParameter("application"));
            assertEquals("sysowner", url.getParameter("owner"));
            assertEquals("dubbo", url.getProtocol());
            assertEquals(20819, url.getPort());
            String register = url.getParameter("register");
            assertTrue(register != null && !"".equals(register));
            assertEquals(false, Boolean.valueOf(register));
        } finally {
            System.setProperty("dubbo.application.name", "");
            System.setProperty("dubbo.application.owner", "");
            System.setProperty("dubbo.registry.address", "");
            System.setProperty("dubbo.protocol.name", "");
            System.setProperty("dubbo.protocol.port", "");
            System.setProperty("dubbo.service.register", "");
            providerContext.stop();
            providerContext.close();
        }
    }

    @Test
    public void testSystemPropertyOverrideReferenceConfig() throws Exception {
        System.setProperty("dubbo.reference.retries", "5");

        try {
            ServiceConfig<DemoService> service = new ServiceConfig<DemoService>();
            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            service.setRegistry(new RegistryConfig(RegistryConfig.NO_AVAILABLE));
            ProtocolConfig protocolConfig = new ProtocolConfig("injvm");
            service.setProtocol(protocolConfig);
            service.export();

            ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
            reference.setInterface(DemoService.class);
            reference.setInjvm(true);
            reference.setRetries(2);
            reference.get();
            assertEquals(Integer.valueOf(5), reference.getRetries());
        } finally {
            System.setProperty("dubbo.reference.retries", "");
        }
    }

    @Test
    public void testSystemPropertyOverrideApiDefault() throws Exception {
        System.setProperty("dubbo.application.name", "sysover");
        System.setProperty("dubbo.application.owner", "sysowner");
        System.setProperty("dubbo.registry.address", "N/A");
        System.setProperty("dubbo.protocol.name", "dubbo");
        System.setProperty("dubbo.protocol.port", "20834");
        try {
            ServiceConfig<DemoService> serviceConfig = new ServiceConfig<DemoService>();
            serviceConfig.setInterface(DemoService.class);
            serviceConfig.setRef(new DemoServiceImpl());
            serviceConfig.export();
            try {
                assertEquals("sysover", serviceConfig.getApplication().getName());
                assertEquals("sysowner", serviceConfig.getApplication().getOwner());
                assertEquals("N/A", serviceConfig.getRegistry().getAddress());
                assertEquals("dubbo", serviceConfig.getProtocol().getName());
                assertEquals(20834, serviceConfig.getProtocol().getPort().intValue());
            } finally {
                serviceConfig.unexport();
            }
        } finally {
            System.setProperty("dubbo.application.name", "");
            System.setProperty("dubbo.application.owner", "");
            System.setProperty("dubbo.registry.address", "");
            System.setProperty("dubbo.protocol.name", "");
            System.setProperty("dubbo.protocol.port", "");
        }
    }

    @Test
    public void testSystemPropertyOverrideApi() throws Exception {
        System.setProperty("dubbo.application.name", "sysover");
        System.setProperty("dubbo.application.owner", "sysowner");
        System.setProperty("dubbo.registry.address", "N/A");
        System.setProperty("dubbo.protocol.name", "dubbo");
        System.setProperty("dubbo.protocol.port", "20834");
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
            service.export();

            try {
                URL url = service.toUrls().get(0);
                assertEquals("sysover", url.getParameter("application"));
                assertEquals("sysowner", url.getParameter("owner"));
                assertEquals("dubbo", url.getProtocol());
                assertEquals(20834, url.getPort());
            } finally {
                service.unexport();
            }
        } finally {
            System.setProperty("dubbo.application.name", "");
            System.setProperty("dubbo.application.owner", "");
            System.setProperty("dubbo.registry.address", "");
            System.setProperty("dubbo.protocol.name", "");
            System.setProperty("dubbo.protocol.port", "");
        }
    }

    @Test
    public void testSystemPropertyOverrideProperties() throws Exception {
        String portString = System.getProperty("dubbo.protocol.port");
        System.clearProperty("dubbo.protocol.port");
        try {
            int port = 1234;
            System.setProperty("dubbo.protocol.port", String.valueOf(port));
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
            service.export();

            try {
                URL url = service.toUrls().get(0);
                // from api
                assertEquals("aaa", url.getParameter("application"));
                // from dubbo.properties
                assertEquals("world", url.getParameter("owner"));
                // from system property
                assertEquals(1234, url.getPort());
            } finally {
                service.unexport();
            }
        } finally {
            if (portString != null) {
                System.setProperty("dubbo.protocol.port", portString);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomizeParameter() throws Exception {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/customize-parameter.xml");
        context.start();
        ServiceBean<DemoService> serviceBean = (ServiceBean<DemoService>) context.getBean("demoServiceExport");
        URL url = (URL) serviceBean.toUrls().get(0);
        assertEquals("protocol-paramA", url.getParameter("protocol.paramA"));
        assertEquals("service-paramA", url.getParameter("service.paramA"));
    }

    @Test
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
    public void testAnnotation() {
        SimpleRegistryService registryService = new SimpleRegistryService();
        Exporter<RegistryService> exporter = SimpleRegistryExporter.export(4548, registryService);
        try {
            ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/annotation-provider.xml");
            providerContext.start();
            try {
                ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/annotation-consumer.xml");
                consumerContext.start();
                try {
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
            exporter.unexport();
        }
    }

    @Test
    public void testDubboProtocolPortOverride() throws Exception {
        String dubboPort = System.getProperty("dubbo.protocol.dubbo.port");
        int port = 55555;
        System.setProperty("dubbo.protocol.dubbo.port", String.valueOf(port));
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
            service.export();

            Assert.assertEquals(port, service.getExportedUrls().get(0).getPort());
        } finally {
            if (StringUtils.isNotEmpty(dubboPort)) {
                System.setProperty("dubbo.protocol.dubbo.port", dubboPort);
            }
            if (service != null) {
                service.unexport();
            }
        }
    }

    @Test
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
            demoService.export();
            helloService.export();

            Assert.assertEquals(demoService.getExportedUrls().get(0).getPort(),
                    helloService.getExportedUrls().get(0).getPort());
        } finally {
            unexportService(demoService);
            unexportService(helloService);
        }
    }

    @Test
    public void testReferGenericExport() throws Exception {
        ApplicationConfig ac = new ApplicationConfig("test-refer-generic-export");
        RegistryConfig rc = new RegistryConfig();
        rc.setAddress(RegistryConfig.NO_AVAILABLE);

        ServiceConfig<GenericService> sc = new ServiceConfig<GenericService>();
        sc.setApplication(ac);
        sc.setRegistry(rc);
        sc.setInterface(DemoService.class.getName());
        sc.setRef(new GenericService() {

            public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
                return null;
            }
        });

        ReferenceConfig<DemoService> ref = new ReferenceConfig<DemoService>();
        ref.setApplication(ac);
        ref.setRegistry(rc);
        ref.setInterface(DemoService.class.getName());

        try {
            sc.export();
            ref.get();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.unexport();
            ref.destroy();
        }
    }

    @Test
    public void testGenericServiceConfig() throws Exception {
        ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
        service.setApplication(new ApplicationConfig("test"));
        service.setRegistry(new RegistryConfig("mock://localhost"));
        service.setInterface(DemoService.class.getName());
        service.setGeneric(Constants.GENERIC_SERIALIZATION_BEAN);
        service.setRef(new GenericService() {

            public Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException {
                return null;
            }
        });
        try {
            service.export();
            Collection<Registry> collection = MockRegistryFactory.getCachedRegistry();
            MockRegistry registry = (MockRegistry) collection.iterator().next();
            URL url = registry.getRegistered().get(0);
            Assert.assertEquals(Constants.GENERIC_SERIALIZATION_BEAN, url.getParameter(Constants.GENERIC_KEY));
        } finally {
            MockRegistryFactory.cleanCachedRegistry();
            service.unexport();
        }
    }

    @Test
    public void testGenericServiceConfigThroughSpring() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/generic-export.xml");
        try {
            ctx.start();
            ServiceConfig serviceConfig = (ServiceConfig) ctx.getBean("dubboDemoService");
            URL url = (URL) serviceConfig.getExportedUrls().get(0);
            Assert.assertEquals(Constants.GENERIC_SERIALIZATION_BEAN, url.getParameter(Constants.GENERIC_KEY));
        } finally {
            ctx.destroy();
        }
    }
}