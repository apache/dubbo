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
import org.testcontainers.shaded.org.hamcrest.CoreMatchers;
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.PREFERRED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.config.Constants.SHUTDOWN_TIMEOUT_KEY;

class RegistryConfigTest {

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    void testProtocol() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setProtocol("protocol");
        MatcherAssert.assertThat(registry.getProtocol(), Matchers.equalTo(registry.getProtocol()));
    }

    @Test
    void testAddress() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://mrh:123@localhost:9103/registry?backup=localhost:9104&k1=v1");
        MatcherAssert.assertThat(registry.getAddress(), Matchers.equalTo("zookeeper://mrh:123@localhost:9103/registry?backup=localhost:9104&k1=v1"));
        MatcherAssert.assertThat(registry.getProtocol(), Matchers.equalTo("zookeeper"));
        MatcherAssert.assertThat(registry.getUsername(), Matchers.equalTo("mrh"));
        MatcherAssert.assertThat(registry.getPassword(), Matchers.equalTo("123"));
        MatcherAssert.assertThat(registry.getParameters().get("k1"), Matchers.equalTo("v1"));
        Map<String, String> parameters = new HashMap<>();
        RegistryConfig.appendParameters(parameters, registry);
        MatcherAssert.assertThat(parameters, Matchers.not(Matchers.hasKey("address")));
    }

    @Test
    void testUsername() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setUsername("username");
        MatcherAssert.assertThat(registry.getUsername(), Matchers.equalTo("username"));
    }

    @Test
    void testPassword() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setPassword("password");
        MatcherAssert.assertThat(registry.getPassword(), Matchers.equalTo("password"));
    }

    @Test
    void testWait() throws Exception {
        try {
            RegistryConfig registry = new RegistryConfig();
            registry.setWait(10);
            MatcherAssert.assertThat(registry.getWait(), CoreMatchers.is(10));
            MatcherAssert.assertThat(System.getProperty(SHUTDOWN_WAIT_KEY), Matchers.equalTo("10"));
        } finally {
            System.clearProperty(SHUTDOWN_TIMEOUT_KEY);
        }
    }

    @Test
    void testCheck() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setCheck(true);
        MatcherAssert.assertThat(registry.isCheck(), CoreMatchers.is(true));
    }

    @Test
    void testFile() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setFile("file");
        MatcherAssert.assertThat(registry.getFile(), Matchers.equalTo("file"));
    }

    @Test
    void testTransporter() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setTransporter("transporter");
        MatcherAssert.assertThat(registry.getTransporter(), Matchers.equalTo("transporter"));
    }

    @Test
    void testClient() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setClient("client");
        MatcherAssert.assertThat(registry.getClient(), Matchers.equalTo("client"));
    }

    @Test
    void testTimeout() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setTimeout(10);
        MatcherAssert.assertThat(registry.getTimeout(), CoreMatchers.is(10));
    }

    @Test
    void testSession() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setSession(10);
        MatcherAssert.assertThat(registry.getSession(), CoreMatchers.is(10));
    }

    @Test
    void testDynamic() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setDynamic(true);
        MatcherAssert.assertThat(registry.isDynamic(), CoreMatchers.is(true));
    }

    @Test
    void testRegister() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setRegister(true);
        MatcherAssert.assertThat(registry.isRegister(), CoreMatchers.is(true));
    }

    @Test
    void testSubscribe() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setSubscribe(true);
        MatcherAssert.assertThat(registry.isSubscribe(), CoreMatchers.is(true));
    }

    @Test
    void testCluster() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setCluster("cluster");
        MatcherAssert.assertThat(registry.getCluster(), Matchers.equalTo("cluster"));
    }

    @Test
    void testGroup() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setGroup("group");
        MatcherAssert.assertThat(registry.getGroup(), Matchers.equalTo("group"));
    }

    @Test
    void testVersion() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setVersion("1.0.0");
        MatcherAssert.assertThat(registry.getVersion(), Matchers.equalTo("1.0.0"));
    }

    @Test
    void testParameters() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setParameters(Collections.singletonMap("k1", "v1"));
        MatcherAssert.assertThat(registry.getParameters(), Matchers.hasEntry("k1", "v1"));
        Map<String, String> parameters = new HashMap<String, String>();
        RegistryConfig.appendParameters(parameters, registry);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry("k1", "v1"));
    }

    @Test
    void testDefault() throws Exception {
        RegistryConfig registry = new RegistryConfig();
        registry.setDefault(true);
        MatcherAssert.assertThat(registry.isDefault(), CoreMatchers.is(true));
    }

    @Test
    void testEquals() throws Exception {
        RegistryConfig registry1 = new RegistryConfig();
        RegistryConfig registry2 = new RegistryConfig();
        registry1.setAddress(ZookeeperRegistryCenterConfig.getConnectionAddress2());
        registry2.setAddress("zookeeper://127.0.0.1:2183");
        Assertions.assertNotEquals(registry1, registry2);
    }

    @Test
    void testMetaData() {
        RegistryConfig config = new RegistryConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: " + metaData);
    }

    @Test
    void testOverrideConfigBySystemProps() {

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
    void testPreferredWithFalseValue() {
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
