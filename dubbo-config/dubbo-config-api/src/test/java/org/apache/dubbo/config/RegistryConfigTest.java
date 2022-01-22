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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.PREFERRED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.config.Constants.SHUTDOWN_TIMEOUT_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

public class RegistryConfigTest {

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    public void testProtocol() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setProtocol("protocol");
        assertThat(registry.getProtocol(), equalTo(registry.getProtocol()));
    }

    @Test
    public void testAddress() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("localhost");
        assertThat(registry.getAddress(), equalTo("localhost"));
        Map<String, String> parameters = new HashMap<String, String>();
        RegistryConfig.appendParameters(parameters, registry);
        assertThat(parameters, not(hasKey("address")));
    }

    @Test
    public void testUsername() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setUsername("username");
        assertThat(registry.getUsername(), equalTo("username"));
    }

    @Test
    public void testPassword() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setPassword("password");
        assertThat(registry.getPassword(), equalTo("password"));
    }

    @Test
    public void testWait() throws Exception {
        try {
            RegistryConfig registry = new RegistryConfig();
            registry.setWait(10);
            assertThat(registry.getWait(), is(10));
            assertThat(System.getProperty(SHUTDOWN_WAIT_KEY), equalTo("10"));
        } finally {
            System.clearProperty(SHUTDOWN_TIMEOUT_KEY);
        }
    }

    @Test
    public void testCheck() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setCheck(true);
        assertThat(registry.isCheck(), is(true));
    }

    @Test
    public void testFile() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setFile("file");
        assertThat(registry.getFile(), equalTo("file"));
    }

    @Test
    public void testTransporter() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setTransporter("transporter");
        assertThat(registry.getTransporter(), equalTo("transporter"));
    }

    @Test
    public void testClient() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setClient("client");
        assertThat(registry.getClient(), equalTo("client"));
    }

    @Test
    public void testTimeout() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setTimeout(10);
        assertThat(registry.getTimeout(), is(10));
    }

    @Test
    public void testSession() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setSession(10);
        assertThat(registry.getSession(), is(10));
    }

    @Test
    public void testDynamic() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setDynamic(true);
        assertThat(registry.isDynamic(), is(true));
    }

    @Test
    public void testRegister() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setRegister(true);
        assertThat(registry.isRegister(), is(true));
    }

    @Test
    public void testSubscribe() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setSubscribe(true);
        assertThat(registry.isSubscribe(), is(true));
    }

    @Test
    public void testCluster() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setCluster("cluster");
        assertThat(registry.getCluster(), equalTo("cluster"));
    }

    @Test
    public void testGroup() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setGroup("group");
        assertThat(registry.getGroup(), equalTo("group"));
    }

    @Test
    public void testVersion() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setVersion("1.0.0");
        assertThat(registry.getVersion(), equalTo("1.0.0"));
    }

    @Test
    public void testParameters() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setParameters(Collections.singletonMap("k1", "v1"));
        assertThat(registry.getParameters(), hasEntry("k1", "v1"));
        Map<String, String> parameters = new HashMap<String, String>();
        RegistryConfig.appendParameters(parameters, registry);
        assertThat(parameters, hasEntry("k1", "v1"));
    }

    @Test
    public void testDefault() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setDefault(true);
        assertThat(registry.isDefault(), is(true));
    }

    @Test
    public void testEquals() throws Exception {
        RegistryConfig registry1 = new RegistryConfig();
        RegistryConfig registry2 = new RegistryConfig();
        registry1.setAddress(ZookeeperRegistryCenterConfig.getConnectionAddress2());
        registry2.setAddress("zookeeper://127.0.0.1:2183");
        Assertions.assertNotEquals(registry1, registry2);
    }

    @Test
    public void testMetaData() {
        RegistryConfig config = new RegistryConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: " + metaData);
    }

    @Test
    public void testOverrideConfigBySystemProps() {

        SysProps.setProperty("dubbo.registry.address", "zookeeper://${zookeeper.address}:${zookeeper.port}");
        SysProps.setProperty("dubbo.registry.useAsConfigCenter", "false");
        SysProps.setProperty("dubbo.registry.useAsMetadataCenter", "false");
        SysProps.setProperty("zookeeper.address", "localhost");
        SysProps.setProperty("zookeeper.port", "2188");


        DubboBootstrap.getInstance()
            .application("demo-app")
            .initialize();
        Collection<RegistryConfig> registries = ApplicationModel.defaultModel().getApplicationConfigManager().getRegistries();
        Assertions.assertEquals(1, registries.size());
        RegistryConfig registryConfig = registries.iterator().next();
        Assertions.assertEquals("zookeeper://localhost:2188", registryConfig.getAddress());

    }

    public void testPreferredWithTrueValue() {
        RegistryConfig registry = new RegistryConfig();
        registry.setPreferred(true);
        Map<String, String> map = new HashMap<>();
        // process Parameter annotation
        AbstractConfig.appendParameters(map, registry);
        // Simulate the check that ZoneAwareClusterInvoker#doInvoke do
        URL url = UrlUtils.parseURL(ZookeeperRegistryCenterConfig.getConnectionAddress1(), map);
        Assertions.assertTrue(url.getParameter(PREFERRED_KEY, false));
    }

    @Test
    public void testPreferredWithFalseValue() {
        RegistryConfig registry = new RegistryConfig();
        registry.setPreferred(false);
        Map<String, String> map = new HashMap<>();
        // Process Parameter annotation
        AbstractConfig.appendParameters(map, registry);
        // Simulate the check that ZoneAwareClusterInvoker#doInvoke do
        URL url = UrlUtils.parseURL(ZookeeperRegistryCenterConfig.getConnectionAddress1(), map);
        Assertions.assertFalse(url.getParameter(PREFERRED_KEY, false));
    }

}
