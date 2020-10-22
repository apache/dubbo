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
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.spring.ConfigTest;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.impl.DemoServiceImpl;
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

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DubboNamespaceHandlerTest {
    @BeforeEach
    public void setUp() {
        ApplicationModel.reset();
    }

    @AfterEach
    public void tearDown() {
        ApplicationModel.reset();
    }

    @Configuration
    @PropertySource("classpath:/META-INF/demo-provider.properties")
    @ImportResource(locations = "classpath:/org/apache/dubbo/config/spring/demo-provider.xml")
    static class XmlConfiguration {

    }

    @Test
    public void testProviderXmlOnConfigurationClass() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(XmlConfiguration.class);
        applicationContext.refresh();
        testProviderXml(applicationContext);
    }

    @Test
    public void testProviderXml() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider.xml",
                ConfigTest.class.getPackage().getName().replace('.', '/') + "/demo-provider-properties.xml"
        );
        ctx.start();

        testProviderXml(ctx);
    }

    private void testProviderXml(ApplicationContext context) {
        ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
        assertThat(protocolConfig, not(nullValue()));
        assertThat(protocolConfig.getName(), is("dubbo"));
        assertThat(protocolConfig.getPort(), is(20813));

        ApplicationConfig applicationConfig = context.getBean(ApplicationConfig.class);
        assertThat(applicationConfig, not(nullValue()));
        assertThat(applicationConfig.getName(), is("demo-provider"));

        DemoService service = context.getBean(DemoService.class);
        assertThat(service, not(nullValue()));
    }

    @Test
    public void testMultiProtocol() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-protocol.xml");
        ctx.start();

        Map<String, ProtocolConfig> protocolConfigMap = ctx.getBeansOfType(ProtocolConfig.class);
        assertThat(protocolConfigMap.size(), is(2));

        ProtocolConfig rmiProtocolConfig = protocolConfigMap.get("rmi");
        assertThat(rmiProtocolConfig.getPort(), is(10991));

        ProtocolConfig dubboProtocolConfig = protocolConfigMap.get("dubbo");
        assertThat(dubboProtocolConfig.getPort(), is(20881));
    }

    @Test
    public void testDefaultProtocol() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/override-protocol.xml");
        ctx.start();

        ProtocolConfig protocolConfig = ctx.getBean(ProtocolConfig.class);
        protocolConfig.refresh();
        assertThat(protocolConfig.getName(), is("dubbo"));
    }

    @Test
    public void testCustomParameter() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/customize-parameter.xml");
        ctx.start();

        ProtocolConfig protocolConfig = ctx.getBean(ProtocolConfig.class);
        assertThat(protocolConfig.getParameters().size(), is(1));
        assertThat(protocolConfig.getParameters().get("protocol-paramA"), is("protocol-paramA"));

        ServiceBean serviceBean = ctx.getBean(ServiceBean.class);
        assertThat(serviceBean.getParameters().size(), is(1));
        assertThat(serviceBean.getParameters().get("service-paramA"), is("service-paramA"));
    }


    @Test
    public void testDelayFixedTime() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:/" + ConfigTest.class.getPackage().getName().replace('.', '/') + "/delay-fixed-time.xml");
        ctx.start();

        assertThat(ctx.getBean(ServiceBean.class).getDelay(), is(300));
    }

    @Test
    public void testTimeoutConfig() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/provider-nested-service.xml");
        ctx.start();

        Map<String, ProviderConfig> providerConfigMap = ctx.getBeansOfType(ProviderConfig.class);

        assertThat(providerConfigMap.get("org.apache.dubbo.config.ProviderConfig").getTimeout(), is(2000));
    }

    @Test
    public void testMonitor() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/provider-with-monitor.xml");
        ctx.start();

        assertThat(ctx.getBean(MonitorConfig.class), not(nullValue()));
    }

//    @Test
//    public void testMultiMonitor() {
//        Assertions.assertThrows(BeanCreationException.class, () -> {
//            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/multi-monitor.xml");
//            ctx.start();
//        });
//    }
//
//    @Test
//    public void testMultiProviderConfig() {
//        Assertions.assertThrows(BeanCreationException.class, () -> {
//            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/provider-multi.xml");
//            ctx.start();
//        });
//    }

    @Test
    public void testModuleInfo() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/provider-with-module.xml");
        ctx.start();

        ModuleConfig moduleConfig = ctx.getBean(ModuleConfig.class);
        assertThat(moduleConfig.getName(), is("test-module"));
    }

    @Test
    public void testNotificationWithWrongBean() {
        Assertions.assertThrows(BeanCreationException.class, () -> {
            ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/consumer-notification.xml");
            ctx.start();
        });
    }

    @Test
    public void testProperty() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(ConfigTest.class.getPackage().getName().replace('.', '/') + "/service-class.xml");
        ctx.start();

        ServiceBean serviceBean = ctx.getBean(ServiceBean.class);

        String prefix = ((DemoServiceImpl) serviceBean.getRef()).getPrefix();
        assertThat(prefix, is("welcome:"));
    }
}