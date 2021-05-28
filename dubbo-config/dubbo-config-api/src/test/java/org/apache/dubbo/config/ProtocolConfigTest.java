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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class ProtocolConfigTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    public void testName() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        String protocolName = "xprotocol";
        protocol.setName(protocolName);
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        assertThat(protocol.getName(), equalTo(protocolName));
        assertThat(protocol.getId(), equalTo(null));
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testHost() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setHost("host");
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        assertThat(protocol.getHost(), equalTo("host"));
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testPort() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setPort(8080);
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        assertThat(protocol.getPort(), equalTo(8080));
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testPath() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setContextpath("context-path");
        Map<String, String> parameters = new HashMap<String, String>();
        ProtocolConfig.appendParameters(parameters, protocol);
        assertThat(protocol.getPath(), equalTo("context-path"));
        assertThat(protocol.getContextpath(), equalTo("context-path"));
        assertThat(parameters.isEmpty(), is(true));
        protocol.setPath("path");
        assertThat(protocol.getPath(), equalTo("path"));
        assertThat(protocol.getContextpath(), equalTo("path"));
    }

    @Test
    public void testCorethreads() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setCorethreads(10);
        assertThat(protocol.getCorethreads(), is(10));
    }

    @Test
    public void testThreads() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setThreads(10);
        assertThat(protocol.getThreads(), is(10));
    }

    @Test
    public void testIothreads() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setIothreads(10);
        assertThat(protocol.getIothreads(), is(10));
    }

    @Test
    public void testQueues() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setQueues(10);
        assertThat(protocol.getQueues(), is(10));
    }

    @Test
    public void testAccepts() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setAccepts(10);
        assertThat(protocol.getAccepts(), is(10));
    }

    @Test
    public void testCodec() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setCodec("mockcodec");
        assertThat(protocol.getCodec(), equalTo("mockcodec"));
    }

    @Test
    public void testAccesslog() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setAccesslog("access.log");
        assertThat(protocol.getAccesslog(), equalTo("access.log"));
    }

    @Test
    public void testTelnet() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setTelnet("mocktelnethandler");
        assertThat(protocol.getTelnet(), equalTo("mocktelnethandler"));
    }

    @Test
    public void testRegister() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setRegister(true);
        assertThat(protocol.isRegister(), is(true));
    }

    @Test
    public void testTransporter() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setTransporter("mocktransporter");
        assertThat(protocol.getTransporter(), equalTo("mocktransporter"));
    }

    @Test
    public void testExchanger() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setExchanger("mockexchanger");
        assertThat(protocol.getExchanger(), equalTo("mockexchanger"));
    }

    @Test
    public void testDispatcher() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setDispatcher("mockdispatcher");
        assertThat(protocol.getDispatcher(), equalTo("mockdispatcher"));
    }

    @Test
    public void testNetworker() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setNetworker("networker");
        assertThat(protocol.getNetworker(), equalTo("networker"));
    }

    @Test
    public void testParameters() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setParameters(Collections.singletonMap("k1", "v1"));
        assertThat(protocol.getParameters(), hasEntry("k1", "v1"));
    }

    @Test
    public void testDefault() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setDefault(true);
        assertThat(protocol.isDefault(), is(true));
    }

    @Test
    public void testKeepAlive() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setKeepAlive(true);
        assertThat(protocol.getKeepAlive(), is(true));
    }

    @Test
    public void testOptimizer() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setOptimizer("optimizer");
        assertThat(protocol.getOptimizer(), equalTo("optimizer"));
    }

    @Test
    public void testExtension() throws Exception {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setExtension("extension");
        assertThat(protocol.getExtension(), equalTo("extension"));
    }

    @Test
    public void testMetaData() {
        ProtocolConfig config = new ProtocolConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "actual: "+metaData);
    }

    @Test
    public void testOverrideEmptyConfig() {
        //dubbo.protocol.name=rest
        //dubbo.protocol.port=1234
        SysProps.setProperty("dubbo.protocol.name", "rest");
        SysProps.setProperty("dubbo.protocol.port", "1234");

        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();

            DubboBootstrap.getInstance()
                    .application("test-app")
                    .protocol(protocolConfig)
                    .initialize();

            Assertions.assertEquals("rest", protocolConfig.getName());
            Assertions.assertEquals(1234, protocolConfig.getPort());
        } finally {
        }
    }

    @Test
    public void testOverrideConfigByName() {
        SysProps.setProperty("dubbo.protocols.rest.port", "1234");

        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setName("rest");

            DubboBootstrap.getInstance()
                    .application("test-app")
                    .protocol(protocolConfig)
                    .initialize();

            Assertions.assertEquals("rest", protocolConfig.getName());
            Assertions.assertEquals(1234, protocolConfig.getPort());
        } finally {
        }
    }

    @Test
    public void testOverrideConfigById() {
        SysProps.setProperty("dubbo.protocols.rest1.name", "rest");
        SysProps.setProperty("dubbo.protocols.rest1.port", "1234");

        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setName("xxx");
            protocolConfig.setId("rest1");

            DubboBootstrap.getInstance()
                    .application("test-app")
                    .protocol(protocolConfig)
                    .initialize();

            Assertions.assertEquals("rest", protocolConfig.getName());
            Assertions.assertEquals(1234, protocolConfig.getPort());
        } finally {
        }
    }

    @Test
    public void testCreateConfigFromPropsWithId() {
        SysProps.setProperty("dubbo.protocols.rest1.name", "rest");
        SysProps.setProperty("dubbo.protocols.rest1.port", "1234");
        SysProps.setProperty("dubbo.protocol.name", "dubbo"); // ignore
        SysProps.setProperty("dubbo.protocol.port", "2346");

        try {

            DubboBootstrap bootstrap = DubboBootstrap.getInstance();
            bootstrap.application("test-app")
                    .initialize();

            ConfigManager configManager = bootstrap.getConfigManager();
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());

            ProtocolConfig protocol = configManager.getProtocol("rest1").get();

            Assertions.assertEquals("rest", protocol.getName());
            Assertions.assertEquals(1234, protocol.getPort());
        } finally {
        }
    }

    @Test
    public void testCreateConfigFromPropsWithName() {
        SysProps.setProperty("dubbo.protocols.rest.port", "1234");
        SysProps.setProperty("dubbo.protocol.name", "dubbo"); // ignore
        SysProps.setProperty("dubbo.protocol.port", "2346");

        try {

            DubboBootstrap bootstrap = DubboBootstrap.getInstance();
            bootstrap.application("test-app")
                    .initialize();

            ConfigManager configManager = bootstrap.getConfigManager();
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());

            ProtocolConfig protocol = configManager.getProtocol("rest").get();

            Assertions.assertEquals("rest", protocol.getName());
            Assertions.assertEquals(1234, protocol.getPort());
        } finally {
        }
    }

    @Test
    public void testCreateDefaultConfigFromProps() {
        SysProps.setProperty("dubbo.protocol.name", "rest");
        SysProps.setProperty("dubbo.protocol.port", "2346");
        String protocolId = "rest-protocol";
        SysProps.setProperty("dubbo.protocol.id", protocolId); // Allow override config id from props

        try {

            DubboBootstrap bootstrap = DubboBootstrap.getInstance();
            bootstrap.application("test-app")
                    .initialize();

            ConfigManager configManager = bootstrap.getConfigManager();
            Collection<ProtocolConfig> protocols = configManager.getProtocols();
            Assertions.assertEquals(1, protocols.size());

            ProtocolConfig protocol = configManager.getProtocol("rest").get();
            Assertions.assertEquals("rest", protocol.getName());
            Assertions.assertEquals(2346, protocol.getPort());
            Assertions.assertEquals(protocolId, protocol.getId());

            ProtocolConfig protocolConfig = configManager.getProtocol(protocolId).get();

        } finally {
            DubboBootstrap.getInstance().stop();
        }
    }

}
