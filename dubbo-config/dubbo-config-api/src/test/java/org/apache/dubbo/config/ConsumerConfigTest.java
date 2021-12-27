/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ConsumerConfigTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    public void testTimeout() throws Exception {
        System.clearProperty("sun.rmi.transport.tcp.responseTimeout");
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setTimeout(10);
        assertThat(consumer.getTimeout(), is(10));
        assertThat(System.getProperty("sun.rmi.transport.tcp.responseTimeout"), equalTo("10"));
    }

    @Test
    public void testDefault() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setDefault(true);
        assertThat(consumer.isDefault(), is(true));
    }

    @Test
    public void testClient() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setClient("client");
        assertThat(consumer.getClient(), equalTo("client"));
    }

    @Test
    public void testThreadpool() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setThreadpool("fixed");
        assertThat(consumer.getThreadpool(), equalTo("fixed"));
    }

    @Test
    public void testCorethreads() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setCorethreads(10);
        assertThat(consumer.getCorethreads(), equalTo(10));
    }

    @Test
    public void testThreads() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setThreads(20);
        assertThat(consumer.getThreads(), equalTo(20));
    }

    @Test
    public void testQueues() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setQueues(5);
        assertThat(consumer.getQueues(), equalTo(5));
    }

    @Test
    public void testOverrideConfigSingle() {
        SysProps.setProperty("dubbo.consumer.check", "false");
        SysProps.setProperty("dubbo.consumer.group", "demo");
        SysProps.setProperty("dubbo.consumer.threads", "10");

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setGroup("groupA");
        consumerConfig.setThreads(20);
        consumerConfig.setCheck(true);

        DubboBootstrap.getInstance()
                .application("demo-app")
                .consumer(consumerConfig)
                .initialize();

        Collection<ConsumerConfig> consumers = ApplicationModel.defaultModel().getDefaultModule().getConfigManager().getConsumers();
        Assertions.assertEquals(1, consumers.size());
        Assertions.assertEquals(consumerConfig, consumers.iterator().next());
        Assertions.assertEquals(false, consumerConfig.isCheck());
        Assertions.assertEquals("demo", consumerConfig.getGroup());
        Assertions.assertEquals(10, consumerConfig.getThreads());

        DubboBootstrap.getInstance().destroy();

    }

    @Test
    public void testOverrideConfigByPluralityId() {
        SysProps.setProperty("dubbo.consumer.group", "demoA");  //ignore
        SysProps.setProperty("dubbo.consumers.consumerA.check", "false");
        SysProps.setProperty("dubbo.consumers.consumerA.group", "demoB");
        SysProps.setProperty("dubbo.consumers.consumerA.threads", "10");

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setId("consumerA");
        consumerConfig.setGroup("groupA");
        consumerConfig.setThreads(20);
        consumerConfig.setCheck(true);

        DubboBootstrap.getInstance()
                .application("demo-app")
                .consumer(consumerConfig)
                .initialize();

        Collection<ConsumerConfig> consumers = ApplicationModel.defaultModel().getDefaultModule().getConfigManager().getConsumers();
        Assertions.assertEquals(1, consumers.size());
        Assertions.assertEquals(consumerConfig, consumers.iterator().next());
        Assertions.assertEquals(false, consumerConfig.isCheck());
        Assertions.assertEquals("demoB", consumerConfig.getGroup());
        Assertions.assertEquals(10, consumerConfig.getThreads());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    public void testOverrideConfigBySingularId() {
        // override success
        SysProps.setProperty("dubbo.consumer.group", "demoA");
        SysProps.setProperty("dubbo.consumer.threads", "15");
        // ignore singular format: dubbo.{tag-name}.{config-id}.{config-item}={config-value}
        SysProps.setProperty("dubbo.consumer.consumerA.check", "false");
        SysProps.setProperty("dubbo.consumer.consumerA.group", "demoB");
        SysProps.setProperty("dubbo.consumer.consumerA.threads", "10");

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setId("consumerA");
        consumerConfig.setGroup("groupA");
        consumerConfig.setThreads(20);
        consumerConfig.setCheck(true);

        DubboBootstrap.getInstance()
                .application("demo-app")
                .consumer(consumerConfig)
                .initialize();

        Collection<ConsumerConfig> consumers = ApplicationModel.defaultModel().getDefaultModule().getConfigManager().getConsumers();
        Assertions.assertEquals(1, consumers.size());
        Assertions.assertEquals(consumerConfig, consumers.iterator().next());
        Assertions.assertEquals(true, consumerConfig.isCheck());
        Assertions.assertEquals("demoA", consumerConfig.getGroup());
        Assertions.assertEquals(15, consumerConfig.getThreads());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    public void testOverrideConfigByDubboProps() {
        ApplicationModel.defaultModel().getDefaultModule();
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.consumers.consumerA.check", "false");
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.consumers.consumerA.group", "demo");
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.consumers.consumerA.threads", "10");

        try {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.setId("consumerA");
            consumerConfig.setGroup("groupA");

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .consumer(consumerConfig)
                    .initialize();

            Collection<ConsumerConfig> consumers = ApplicationModel.defaultModel().getDefaultModule().getConfigManager().getConsumers();
            Assertions.assertEquals(1, consumers.size());
            Assertions.assertEquals(consumerConfig, consumers.iterator().next());
            Assertions.assertEquals(false, consumerConfig.isCheck());
            Assertions.assertEquals("groupA", consumerConfig.getGroup());
            Assertions.assertEquals(10, consumerConfig.getThreads());
        } finally {
            ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().refresh();
            DubboBootstrap.getInstance().destroy();
        }
    }

    @Test
    public void testReferenceAndConsumerConfigOverlay() {
        SysProps.setProperty("dubbo.consumer.group", "demo");
        SysProps.setProperty("dubbo.consumer.threads", "12");
        SysProps.setProperty("dubbo.consumer.timeout", "1234");
        SysProps.setProperty("dubbo.consumer.init", "false");
        SysProps.setProperty("dubbo.consumer.check", "false");
        SysProps.setProperty("dubbo.registry.address", "N/A");

        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(DemoService.class);

        DubboBootstrap.getInstance()
            .application("demo-app")
            .reference(referenceConfig)
            .initialize();

        Assertions.assertEquals("demo", referenceConfig.getGroup());
        Assertions.assertEquals(1234, referenceConfig.getTimeout());
        Assertions.assertEquals(false, referenceConfig.isInit());
        Assertions.assertEquals(false, referenceConfig.isCheck());

        DubboBootstrap.getInstance().destroy();

    }

    @Test
    public void testMetaData() {
        ConsumerConfig consumerConfig = new ConsumerConfig();
        Map<String, String> metaData = consumerConfig.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }
}
