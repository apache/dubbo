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
package org.apache.dubbo.config.spring.context.annotation;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static com.alibaba.spring.util.BeanRegistrar.hasAlias;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * {@link EnableDubboConfig} Test
 *
 * @since 2.5.8
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EnableDubboConfigTest {

    @Test
    public void testSingle() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        // application
        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());

        // module
        ModuleConfig moduleConfig = context.getBean("moduleBean", ModuleConfig.class);
        Assertions.assertEquals("dubbo-demo-module", moduleConfig.getName());

        // registry
        RegistryConfig registryConfig = context.getBean(RegistryConfig.class);
        Assertions.assertEquals("zookeeper://192.168.99.100:32770", registryConfig.getAddress());

        // protocol
        ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
        Assertions.assertEquals("dubbo", protocolConfig.getName());
        Assertions.assertEquals(Integer.valueOf(20880), protocolConfig.getPort());

        // monitor
        MonitorConfig monitorConfig = context.getBean(MonitorConfig.class);
        Assertions.assertEquals("zookeeper://127.0.0.1:32770", monitorConfig.getAddress());

        // provider
        ProviderConfig providerConfig = context.getBean(ProviderConfig.class);
        Assertions.assertEquals("127.0.0.1", providerConfig.getHost());


        // consumer
        ConsumerConfig consumerConfig = context.getBean(ConsumerConfig.class);
        Assertions.assertEquals("netty", consumerConfig.getClient());

        // asserts aliases
        assertFalse(hasAlias(context, "org.apache.dubbo.config.RegistryConfig#0", "zookeeper"));
        assertFalse(hasAlias(context, "org.apache.dubbo.config.MonitorConfig#0", "zookeeper"));

        context.close();
    }

    @Test
    public void testMultiple() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestMultipleConfig.class);
        context.refresh();

        // application
        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());


        ApplicationConfig applicationBean2 = context.getBean("applicationBean2", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application2", applicationBean2.getName());

        ApplicationConfig applicationBean3 = context.getBean("applicationBean3", ApplicationConfig.class);
        Assertions.assertEquals("dubbo-demo-application3", applicationBean3.getName());

        Map<String, ProtocolConfig> protocolConfigs = context.getBeansOfType(ProtocolConfig.class);

        for (Map.Entry<String, ProtocolConfig> entry : protocolConfigs.entrySet()) {
            ProtocolConfig protocol = entry.getValue();
            Assertions.assertEquals(protocol, context.getBean(protocol.getName(), ProtocolConfig.class));
        }

        // asserts aliases
        assertTrue(hasAlias(context, "applicationBean2", "dubbo-demo-application2"));
        assertTrue(hasAlias(context, "applicationBean3", "dubbo-demo-application3"));

        context.close();

    }

    @EnableDubboConfig
    @PropertySource("META-INF/config.properties")
    private static class TestMultipleConfig {

    }

    @EnableDubboConfig(multiple = false)
    @PropertySource("META-INF/config.properties")
    private static class TestConfig {

    }
}