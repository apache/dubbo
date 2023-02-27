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
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
<<<<<<< HEAD
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ConfigTest;
=======
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfigBase;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.context.ModuleConfigManager;
>>>>>>> origin/3.2
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
import org.apache.dubbo.config.spring.impl.DemoServiceXMLLazyInitImpl1;
import org.apache.dubbo.config.spring.impl.DemoServiceXMLLazyInitImpl2;
import org.apache.dubbo.config.spring.impl.DemoServiceXMLNotLazyInitImpl;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collection;
import java.util.Map;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DubboNamespaceHandlerTest {

    private static String resourcePath = "org.apache.dubbo.config.spring".replace('.', '/');

<<<<<<< HEAD
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DubboNamespaceHandlerTest {
=======
>>>>>>> origin/3.2
    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void tearDown() {
        DubboBootstrap.reset();
    }

    @Configuration
    @PropertySource("classpath:/META-INF/demo-provider.properties")
    @ImportResource(locations = "classpath:/org/apache/dubbo/config/spring/demo-provider.xml")
    static class XmlConfiguration {

    }

    @Test
    void testProviderXmlOnConfigurationClass() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(XmlConfiguration.class);
        applicationContext.refresh();
        testProviderXml(applicationContext);
        applicationContext.close();
    }

    @Test
    void testProviderXml() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                resourcePath + "/demo-provider.xml",
                resourcePath + "/demo-provider-properties.xml"
        );
        ctx.start();

        testProviderXml(ctx);
        ctx.close();
    }

    private void testProviderXml(ApplicationContext context) {

        String appName = "demo-provider";
        Map<String, ApplicationConfig> applicationConfigMap = context.getBeansOfType(ApplicationConfig.class);
        ApplicationConfig providerAppConfig = context.getBean(appName, ApplicationConfig.class);
        assertNotNull(providerAppConfig);
        assertEquals(appName, providerAppConfig.getName());
        assertEquals(appName, providerAppConfig.getId());

        ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
        assertThat(protocolConfig, not(nullValue()));
        assertThat(protocolConfig.getName(), is("dubbo"));
        assertThat(protocolConfig.getPort(), is(20813));

        ApplicationConfig applicationConfig = context.getBean(ApplicationConfig.class);
        assertThat(applicationConfig, not(nullValue()));
        assertThat(applicationConfig.getName(), is("demo-provider"));

        RegistryConfig registryConfig = context.getBean(RegistryConfig.class);
        assertThat(registryConfig, not(nullValue()));
        assertThat(registryConfig.getAddress(), is("N/A"));

        DemoService service = context.getBean(DemoService.class);
        assertThat(service, not(nullValue()));
    }

    @Test
    void testMultiProtocol() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-protocol.xml");
        ctx.start();

        Map<String, ProtocolConfig> protocolConfigMap = ctx.getBeansOfType(ProtocolConfig.class);
        assertThat(protocolConfigMap.size(), is(2));

        ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
        Collection<ProtocolConfig> protocolConfigs = configManager.getProtocols();
        assertThat(protocolConfigs.size(), is(2));

        ProtocolConfig rmiProtocolConfig = configManager.getProtocol("rmi").get();
        assertThat(rmiProtocolConfig.getPort(), is(10991));

        ProtocolConfig dubboProtocolConfig = configManager.getProtocol("dubbo").get();
        assertThat(dubboProtocolConfig.getPort(), is(20881));
        ctx.close();
    }

    @Test
    void testDefaultProtocol() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/override-protocol.xml");
        ctx.start();

        ProtocolConfig protocolConfig = ctx.getBean(ProtocolConfig.class);
        protocolConfig.refresh();
        assertThat(protocolConfig.getName(), is("dubbo"));
        ctx.close();
    }

    @Test
    void testCustomParameter() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/customize-parameter.xml");
        ctx.start();

        ProtocolConfig protocolConfig = ctx.getBean(ProtocolConfig.class);
        assertThat(protocolConfig.getParameters().size(), is(1));
        assertThat(protocolConfig.getParameters().get("protocol-paramA"), is("protocol-paramA"));

        ServiceBean serviceBean = ctx.getBean(ServiceBean.class);
        assertThat(serviceBean.getParameters().size(), is(1));
        assertThat(serviceBean.getParameters().get("service-paramA"), is("service-paramA"));
        ctx.close();
    }


    @Test
    void testDelayFixedTime() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:/" + resourcePath + "/delay-fixed-time.xml");
        ctx.start();

        assertThat(ctx.getBean(ServiceBean.class).getDelay(), is(300));
        ctx.close();
    }

    @Test
    void testTimeoutConfig() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/provider-nested-service.xml");
        ctx.start();

        ModuleConfigManager configManager = ApplicationModel.defaultModel().getDefaultModule().getConfigManager();
        Collection<ProviderConfig> providerConfigs = configManager.getProviders();
        Assertions.assertEquals(2, providerConfigs.size());

        ProviderConfig defaultProvider = configManager.getDefaultProvider().get();
        assertThat(defaultProvider.getTimeout(), is(2000));

        ProviderConfig provider2 = configManager.getProvider("provider2").get();

        ServiceConfigBase<Object> serviceConfig2 = configManager.getService("serviceConfig2");
        Assertions.assertEquals(1000, provider2.getTimeout());
        Assertions.assertEquals(provider2.getTimeout(), serviceConfig2.getTimeout());

<<<<<<< HEAD
        assertThat(providerConfigMap.get("org.apache.dubbo.config.ProviderConfig").getTimeout(), is(2000));
        ctx.close();
=======
>>>>>>> origin/3.2
    }

    @Test
    void testMonitor() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/provider-with-monitor.xml");
        ctx.start();

        assertThat(ctx.getBean(MonitorConfig.class), not(nullValue()));
        ctx.close();
    }

//    @Test
//    public void testMultiMonitor() {
//        Assertions.assertThrows(BeanCreationException.class, () -> {
//            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/multi-monitor.xml");
//            ctx.start();
//        });
//    }
//
//    @Test
//    public void testMultiProviderConfig() {
//        Assertions.assertThrows(BeanCreationException.class, () -> {
//            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/provider-multi.xml");
//            ctx.start();
//        });
//    }

    @Test
    void testModuleInfo() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/provider-with-module.xml");
        ctx.start();

        ModuleConfig moduleConfig = ctx.getBean(ModuleConfig.class);
        assertThat(moduleConfig.getName(), is("test-module"));
        ctx.close();
    }

    @Test
    void testNotificationWithWrongBean() {
        Assertions.assertThrows(BeanCreationException.class, () -> {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/consumer-notification.xml");
            ctx.start();
        });
    }

    @Test
    void testProperty() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/service-class.xml");
        ctx.start();

        ServiceBean serviceBean = ctx.getBean(ServiceBean.class);

        String prefix = ((DemoServiceImpl) serviceBean.getRef()).getPrefix();
        assertThat(prefix, is("welcome:"));
        ctx.close();
    }


    @Test
    public void testDuplicateServiceBean() {
        try {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:/org/apache/dubbo/config/spring/demo-provider-duplicate-service-bean.xml");
            ctx.start();
            ctx.stop();
            ctx.close();
        } catch (IllegalStateException e) {
            Assertions.assertTrue(e.getMessage().contains("There are multiple ServiceBean instances with the same service name"));
            return;
        }
        Assertions.fail();
    }

    @Test
    public void testLazyInitServiceBean() {
        ClassPathXmlApplicationContext ctx = null;
        try {
            ctx = new ClassPathXmlApplicationContext("classpath:/org/apache/dubbo/config/spring/demo-provider-lazy-init.xml");
            ctx.start();
            /**
             * {@link DemoServiceXMLLazyInitImpl1} sets lazy-init
             * */
            Assertions.assertEquals(DemoServiceXMLLazyInitImpl1.isInitialized(),false);
            /**
             * {@link DemoServiceXMLLazyInitImpl2} sets default-lazy-init
             * */
            Assertions.assertEquals(DemoServiceXMLLazyInitImpl2.isInitialized(),false);

            /**
             * After getBean and then {@link DemoServiceXMLLazyInitImpl1#isInitialized()} changed to {@code true}
             * */
            ctx.getBean("demoServiceXMLLazyInitImpl1",DemoServiceXMLLazyInitImpl1.class);
            Assertions.assertEquals(DemoServiceXMLLazyInitImpl1.isInitialized(),true);
        }finally {
            if (ctx != null) {
                ctx.stop();
                ctx.close();
            }
        }
    }

    @Test
    public void testNotLazyInitServiceBean() {
        ClassPathXmlApplicationContext ctx = null;
        try {
            ctx = new ClassPathXmlApplicationContext("classpath:/org/apache/dubbo/config/spring/demo-provider-not-lazy-init.xml");
            ctx.start();
            /**
             * {@link DemoServiceXMLNotLazyInitImpl} don't set lazy-init
             * */
            Assertions.assertEquals(DemoServiceXMLNotLazyInitImpl.isInitialized(),true);
        }finally {
            if (ctx != null) {
                ctx.stop();
                ctx.close();
            }
        }
    }

    @Test
    void testMetricsAggregation() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/metrics-aggregation.xml");
        ctx.start();

        ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();

        MetricsConfig metricsBean = ctx.getBean(MetricsConfig.class);
        MetricsConfig metrics = configManager.getMetrics().get();

        assertTrue(metrics.getEnableJvmMetrics());

        assertEquals(metrics.getAggregation().getEnabled(), true);
        assertEquals(metrics.getAggregation().getBucketNum(), 5);
        assertEquals(metrics.getAggregation().getTimeWindowSeconds(), 120);

        assertEquals(metrics.getAggregation().getEnabled(), metricsBean.getAggregation().getEnabled());
        assertEquals(metrics.getAggregation().getBucketNum(), metricsBean.getAggregation().getBucketNum());
        assertEquals(metrics.getAggregation().getTimeWindowSeconds(), metricsBean.getAggregation().getTimeWindowSeconds());
    }

    @Test
    void testMetricsPrometheus() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(resourcePath + "/metrics-prometheus.xml");
        ctx.start();

        ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();

        MetricsConfig metricsBean = ctx.getBean(MetricsConfig.class);
        MetricsConfig metrics = configManager.getMetrics().get();

        assertEquals(metrics.getProtocol(), PROTOCOL_PROMETHEUS);
        assertEquals(metrics.getPrometheus().getExporter().getEnabled(), true);
        assertEquals(metrics.getPrometheus().getExporter().getEnableHttpServiceDiscovery(), true);
        assertEquals(metrics.getPrometheus().getExporter().getHttpServiceDiscoveryUrl(), "localhost:8080");
        assertEquals(metrics.getPrometheus().getExporter().getMetricsPort(), 20888);
        assertEquals(metrics.getPrometheus().getExporter().getMetricsPath(), "/metrics");
        assertEquals(metrics.getPrometheus().getPushgateway().getEnabled(), true);
        assertEquals(metrics.getPrometheus().getPushgateway().getBaseUrl(), "localhost:9091");
        assertEquals(metrics.getPrometheus().getPushgateway().getPushInterval(), 30);
        assertEquals(metrics.getPrometheus().getPushgateway().getUsername(), "username");
        assertEquals(metrics.getPrometheus().getPushgateway().getPassword(), "password");
        assertEquals(metrics.getPrometheus().getPushgateway().getJob(), "job");

        assertEquals(metricsBean.getProtocol(), PROTOCOL_PROMETHEUS);
        assertEquals(metricsBean.getPrometheus().getExporter().getEnabled(), true);
        assertEquals(metricsBean.getPrometheus().getExporter().getEnableHttpServiceDiscovery(), true);
        assertEquals(metricsBean.getPrometheus().getExporter().getHttpServiceDiscoveryUrl(), "localhost:8080");
        assertEquals(metricsBean.getPrometheus().getExporter().getMetricsPort(), 20888);
        assertEquals(metricsBean.getPrometheus().getExporter().getMetricsPath(), "/metrics");
        assertEquals(metricsBean.getPrometheus().getPushgateway().getEnabled(), true);
        assertEquals(metricsBean.getPrometheus().getPushgateway().getBaseUrl(), "localhost:9091");
        assertEquals(metricsBean.getPrometheus().getPushgateway().getPushInterval(), 30);
        assertEquals(metricsBean.getPrometheus().getPushgateway().getUsername(), "username");
        assertEquals(metricsBean.getPrometheus().getPushgateway().getPassword(), "password");
        assertEquals(metricsBean.getPrometheus().getPushgateway().getJob(), "job");
    }
}
