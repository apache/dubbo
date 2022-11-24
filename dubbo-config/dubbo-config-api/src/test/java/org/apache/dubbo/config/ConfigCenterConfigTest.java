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

package org.apache.dubbo.config;


import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

class ConfigCenterConfigTest {

    @BeforeEach
    public void setUp() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    void testPrefix() {
        ConfigCenterConfig config = new ConfigCenterConfig();
        Assertions.assertEquals(Arrays.asList("dubbo.config-center"), config.getPrefixes());

        config.setId("configcenterA");
        Assertions.assertEquals(Arrays.asList("dubbo.config-centers.configcenterA", "dubbo.config-center"),
                config.getPrefixes());
    }

    @Test
    void testToUrl() {
        ConfigCenterConfig config = new ConfigCenterConfig();
        config.setNamespace("namespace");
        config.setGroup("group");
        config.setAddress(ZookeeperRegistryCenterConfig.getConnectionAddress());
        config.setHighestPriority(null);
        config.refresh();

        Assertions.assertEquals(ZookeeperRegistryCenterConfig.getConnectionAddress() +
                "/" + ConfigCenterConfig.class.getName() +
                "?check=true&config-file=dubbo.properties&group=group&namespace=namespace&timeout=30000",
            config.toUrl().toFullString()
        );
    }

    @Test
    void testOverrideConfig() {

        String zkAddr = ZookeeperRegistryCenterConfig.getConnectionAddress();
        // sysprops has no id
        SysProps.setProperty("dubbo.config-center.check", "false");
        SysProps.setProperty("dubbo.config-center.address", zkAddr);

        //No id and no address
        ConfigCenterConfig configCenter = new ConfigCenterConfig();
        configCenter.setAddress("N/A");

        try {
            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .configCenter(configCenter)
                    .initialize();
        } catch (Exception e) {
            // ignore
            e.printStackTrace();
        }

        Collection<ConfigCenterConfig> configCenters = ApplicationModel.defaultModel().getApplicationConfigManager().getConfigCenters();
        Assertions.assertEquals(1, configCenters.size());
        Assertions.assertEquals(configCenter, configCenters.iterator().next());
        Assertions.assertEquals(zkAddr, configCenter.getAddress());
        Assertions.assertEquals(false, configCenter.isCheck());

        DubboBootstrap.getInstance().stop();
    }

    @Test
    void testOverrideConfig2() {

        String zkAddr = "nacos://127.0.0.1:8848";
        // sysprops has no id
        SysProps.setProperty("dubbo.config-center.check", "false");
        SysProps.setProperty("dubbo.config-center.address", zkAddr);

        //No id but has address
        ConfigCenterConfig configCenter = new ConfigCenterConfig();
        configCenter.setAddress(ZookeeperRegistryCenterConfig.getConnectionAddress());

        DubboBootstrap.getInstance()
                .application("demo-app")
                .configCenter(configCenter)
                .start();

        Collection<ConfigCenterConfig> configCenters = ApplicationModel.defaultModel().getApplicationConfigManager().getConfigCenters();
        Assertions.assertEquals(1, configCenters.size());
        Assertions.assertEquals(configCenter, configCenters.iterator().next());
        Assertions.assertEquals(zkAddr, configCenter.getAddress());
        Assertions.assertEquals(false, configCenter.isCheck());

        DubboBootstrap.getInstance().stop();
    }

    @Test
    void testOverrideConfigBySystemProps() {

        //Config instance has Id, but sysprops without id
        SysProps.setProperty("dubbo.config-center.check", "false");
        SysProps.setProperty("dubbo.config-center.timeout", "1234");

        // Config instance has id
        ConfigCenterConfig configCenter = new ConfigCenterConfig();
        configCenter.setTimeout(3000L);

        DubboBootstrap.getInstance()
                .application("demo-app")
                .configCenter(configCenter)
                .initialize();

        Collection<ConfigCenterConfig> configCenters = ApplicationModel.defaultModel().getApplicationConfigManager().getConfigCenters();
        Assertions.assertEquals(1, configCenters.size());
        Assertions.assertEquals(configCenter, configCenters.iterator().next());
        Assertions.assertEquals(1234, configCenter.getTimeout());
        Assertions.assertEquals(false, configCenter.isCheck());

        DubboBootstrap.getInstance().stop();
    }

    @Test
    void testOverrideConfigByDubboProps() {

        ApplicationModel.defaultModel().getDefaultModule();
        // Config instance has id, dubbo props has no id
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.config-center.check", "false");
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.config-center.timeout", "1234");

        try {
            // Config instance has id
            ConfigCenterConfig configCenter = new ConfigCenterConfig();
            configCenter.setTimeout(3000L);

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .configCenter(configCenter)
                    .initialize();

            Collection<ConfigCenterConfig> configCenters = ApplicationModel.defaultModel().getApplicationConfigManager().getConfigCenters();
            Assertions.assertEquals(1, configCenters.size());
            Assertions.assertEquals(configCenter, configCenters.iterator().next());
            Assertions.assertEquals(3000L, configCenter.getTimeout());
            Assertions.assertEquals(false, configCenter.isCheck());
        } finally {
            ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().refresh();
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testOverrideConfigBySystemPropsWithId() {

        // Both config instance and sysprops have id
        SysProps.setProperty("dubbo.config-centers.configcenterA.check", "false");
        SysProps.setProperty("dubbo.config-centers.configcenterA.timeout", "1234");

        // Config instance has id
        ConfigCenterConfig configCenter = new ConfigCenterConfig();
        configCenter.setId("configcenterA");
        configCenter.setTimeout(3000L);

        DubboBootstrap.getInstance()
                .application("demo-app")
                .configCenter(configCenter)
                .start();

        Collection<ConfigCenterConfig> configCenters = ApplicationModel.defaultModel().getApplicationConfigManager().getConfigCenters();
        Assertions.assertEquals(1, configCenters.size());
        Assertions.assertEquals(configCenter, configCenters.iterator().next());
        Assertions.assertEquals(1234, configCenter.getTimeout());
        Assertions.assertEquals(false, configCenter.isCheck());

        DubboBootstrap.getInstance().stop();
    }

    @Test
    void testOverrideConfigByDubboPropsWithId() {

        ApplicationModel.defaultModel().getDefaultModule();
        // Config instance has id, dubbo props has id
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.config-centers.configcenterA.check", "false");
        ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().setProperty("dubbo.config-centers.configcenterA.timeout", "1234");

        try {
            // Config instance has id
            ConfigCenterConfig configCenter = new ConfigCenterConfig();
            configCenter.setId("configcenterA");
            configCenter.setTimeout(3000L);

            DubboBootstrap.getInstance()
                    .application("demo-app")
                    .configCenter(configCenter)
                    .start();

            Collection<ConfigCenterConfig> configCenters = ApplicationModel.defaultModel().getApplicationConfigManager().getConfigCenters();
            Assertions.assertEquals(1, configCenters.size());
            Assertions.assertEquals(configCenter, configCenters.iterator().next());
            Assertions.assertEquals(3000L, configCenter.getTimeout());
            Assertions.assertEquals(false, configCenter.isCheck());
        } finally {
            ApplicationModel.defaultModel().getModelEnvironment().getPropertiesConfiguration().refresh();
            DubboBootstrap.getInstance().stop();
        }
    }

    @Test
    void testMetaData() {
        ConfigCenterConfig configCenter = new ConfigCenterConfig();
        Map<String, String> metaData = configCenter.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }

    @Test
    void testParameters() {
        ConfigCenterConfig cc = new ConfigCenterConfig();
        cc.setParameters(new LinkedHashMap<>());
        cc.getParameters().put(CLIENT_KEY, null);

        Map<String, String> params = new LinkedHashMap<>();
        ConfigCenterConfig.appendParameters(params, cc);

        Map<String, String> attributes = new LinkedHashMap<>();
        ConfigCenterConfig.appendAttributes(attributes, cc);

        String encodedParametersStr = attributes.get("parameters");
        Assertions.assertEquals("[]", encodedParametersStr);
        Assertions.assertEquals(0, StringUtils.parseParameters(encodedParametersStr).size());
    }

    @Test
    void testAttributes() {
        ConfigCenterConfig cc = new ConfigCenterConfig();
        cc.setAddress(ZookeeperRegistryCenterConfig.getConnectionAddress());
        Map<String, String> attributes = new LinkedHashMap<>();
        ConfigCenterConfig.appendAttributes(attributes, cc);

        Assertions.assertEquals(cc.getAddress(), attributes.get("address"));
        Assertions.assertEquals(cc.getProtocol(), attributes.get("protocol"));
        Assertions.assertEquals(""+cc.getPort(), attributes.get("port"));
        Assertions.assertEquals(null, attributes.get("valid"));
        Assertions.assertEquals(null, attributes.get("refreshed"));

    }

    @Test
    void testSetAddress() {
        String address = ZookeeperRegistryCenterConfig.getConnectionAddress();
        ConfigCenterConfig cc = new ConfigCenterConfig();
        cc.setUsername("user123"); // set username first
        cc.setPassword("pass123");
        cc.setAddress(address); // set address last, expect did not override username/password

        Assertions.assertEquals(address, cc.getAddress());
        Assertions.assertEquals("zookeeper", cc.getProtocol());
        Assertions.assertEquals(2181, cc.getPort());
        Assertions.assertEquals("user123", cc.getUsername());
        Assertions.assertEquals("pass123", cc.getPassword());

    }
}