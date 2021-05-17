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

import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ConsumerConfigTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @Test
    public void testTimeout() throws Exception {
        try {
            System.clearProperty("sun.rmi.transport.tcp.responseTimeout");
            ConsumerConfig consumer = new ConsumerConfig();
            consumer.setTimeout(10);
            assertThat(consumer.getTimeout(), is(10));
            assertThat(System.getProperty("sun.rmi.transport.tcp.responseTimeout"), equalTo("10"));
        } finally {
            System.clearProperty("sun.rmi.transport.tcp.responseTimeout");
        }
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
        System.setProperty("dubbo.consumer.check", "false");
        System.setProperty("dubbo.consumer.group", "demo");
        System.setProperty("dubbo.consumer.threads", "10");

        try {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.setGroup("groupA");
            consumerConfig.setThreads(20);
            consumerConfig.setCheck(true);

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .consumer(consumerConfig)
                    .initialize();

            Collection<ConsumerConfig> consumers = ApplicationModel.getConfigManager().getConsumers();
            Assertions.assertEquals(1, consumers.size());
            Assertions.assertEquals(consumerConfig, consumers.iterator().next());
            Assertions.assertEquals(false, consumerConfig.isCheck());
            Assertions.assertEquals("demo", consumerConfig.getGroup());
            Assertions.assertEquals(10, consumerConfig.getThreads());
        } finally {
            System.clearProperty("dubbo.consumer.check");
            System.clearProperty("dubbo.consumer.group");
            System.clearProperty("dubbo.consumer.threads");
        }
    }

    @Test
    public void testOverrideConfigByPluralityId() {
        System.setProperty("dubbo.consumer.group", "demoA");  //ignore
        System.setProperty("dubbo.consumers.consumerA.check", "false");
        System.setProperty("dubbo.consumers.consumerA.group", "demoB");
        System.setProperty("dubbo.consumers.consumerA.threads", "10");

        try {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.setId("consumerA");
            consumerConfig.setGroup("groupA");
            consumerConfig.setThreads(20);
            consumerConfig.setCheck(true);

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .consumer(consumerConfig)
                    .initialize();

            Collection<ConsumerConfig> consumers = ApplicationModel.getConfigManager().getConsumers();
            Assertions.assertEquals(1, consumers.size());
            Assertions.assertEquals(consumerConfig, consumers.iterator().next());
            Assertions.assertEquals(false, consumerConfig.isCheck());
            Assertions.assertEquals("demoB", consumerConfig.getGroup());
            Assertions.assertEquals(10, consumerConfig.getThreads());
        } finally {
            System.clearProperty("dubbo.consumer.group");
            System.clearProperty("dubbo.consumers.consumerA.check");
            System.clearProperty("dubbo.consumers.consumerA.group");
            System.clearProperty("dubbo.consumers.consumerA.threads");
        }
    }

    @Test
    public void testOverrideConfigBySingularId() {
        // override success
        System.setProperty("dubbo.consumer.group", "demoA");
        System.setProperty("dubbo.consumer.threads", "15");
        // ignore singular format: dubbo.{tag-name}.{config-id}.{config-item}={config-value}
        System.setProperty("dubbo.consumer.consumerA.check", "false");
        System.setProperty("dubbo.consumer.consumerA.group", "demoB");
        System.setProperty("dubbo.consumer.consumerA.threads", "10");

        try {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.setId("consumerA");
            consumerConfig.setGroup("groupA");
            consumerConfig.setThreads(20);
            consumerConfig.setCheck(true);

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .consumer(consumerConfig)
                    .initialize();

            Collection<ConsumerConfig> consumers = ApplicationModel.getConfigManager().getConsumers();
            Assertions.assertEquals(1, consumers.size());
            Assertions.assertEquals(consumerConfig, consumers.iterator().next());
            Assertions.assertEquals(true, consumerConfig.isCheck());
            Assertions.assertEquals("demoA", consumerConfig.getGroup());
            Assertions.assertEquals(15, consumerConfig.getThreads());
        } finally {
            System.clearProperty("dubbo.consumer.group");
            System.clearProperty("dubbo.consumer.consumerA.check");
            System.clearProperty("dubbo.consumer.consumerA.group");
            System.clearProperty("dubbo.consumer.consumerA.threads");
        }
    }

    @Test
    public void testOverrideConfigByDubboProps() {
        Map props = new HashMap();
        props.put("dubbo.consumers.consumerA.check", "false");
        props.put("dubbo.consumers.consumerA.group", "demo");
        props.put("dubbo.consumers.consumerA.threads", "10");
        ConfigUtils.getProperties().putAll(props);

        try {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.setId("consumerA");
            //
            consumerConfig.setGroup("groupA");

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .consumer(consumerConfig)
                    .initialize();

            Collection<ConsumerConfig> consumers = ApplicationModel.getConfigManager().getConsumers();
            Assertions.assertEquals(1, consumers.size());
            Assertions.assertEquals(consumerConfig, consumers.iterator().next());
            Assertions.assertEquals(false, consumerConfig.isCheck());
            Assertions.assertEquals("groupA", consumerConfig.getGroup());
            Assertions.assertEquals(10, consumerConfig.getThreads());
        } finally {
            props.keySet().forEach(ConfigUtils.getProperties()::remove);
        }
    }

    @Test
    public void testReferenceAndConsumerConfigOverlay() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("dubbo.consumer.group", "demo");
        props.put("dubbo.consumer.threads", "12");
        props.put("dubbo.consumer.timeout", "1234");
        props.put("dubbo.consumer.init", "false");
        props.put("dubbo.consumer.check", "false");
        props.put("dubbo.registry.address", "N/A");
        System.getProperties().putAll(props);

        try {
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
        } finally {
            props.keySet().forEach(System::clearProperty);
        }

    }

    @Test
    public void testDefaultMetaData() {
        ConsumerConfig consumerConfig = new ConsumerConfig();
        Map<String, String> metaData = consumerConfig.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);

    }
}
