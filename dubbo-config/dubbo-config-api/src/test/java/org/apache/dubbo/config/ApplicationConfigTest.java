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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;

class ApplicationConfigTest {

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    @Test
    void testName() {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("app");
        MatcherAssert.assertThat(application.getName(), Matchers.equalTo("app"));
        MatcherAssert.assertThat(application.getId(), Matchers.equalTo(null));

        application = new ApplicationConfig("app2");
        MatcherAssert.assertThat(application.getName(), Matchers.equalTo("app2"));
        MatcherAssert.assertThat(application.getId(), Matchers.equalTo(null));

        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(APPLICATION_KEY, "app2"));
    }

    @Test
    void testVersion() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setVersion("1.0.0");
        MatcherAssert.assertThat(application.getVersion(), Matchers.equalTo("1.0.0"));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry("application.version", "1.0.0"));
    }

    @Test
    void testOwner() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setOwner("owner");
        MatcherAssert.assertThat(application.getOwner(), Matchers.equalTo("owner"));
    }

    @Test
    void testOrganization() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setOrganization("org");
        MatcherAssert.assertThat(application.getOrganization(), Matchers.equalTo("org"));
    }

    @Test
    void testArchitecture() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setArchitecture("arch");
        MatcherAssert.assertThat(application.getArchitecture(), Matchers.equalTo("arch"));
    }

    @Test
    void testEnvironment1() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setEnvironment("develop");
        MatcherAssert.assertThat(application.getEnvironment(), Matchers.equalTo("develop"));
        application.setEnvironment("test");
        MatcherAssert.assertThat(application.getEnvironment(), Matchers.equalTo("test"));
        application.setEnvironment("product");
        MatcherAssert.assertThat(application.getEnvironment(), Matchers.equalTo("product"));
    }

    @Test
    void testEnvironment2() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ApplicationConfig application = new ApplicationConfig("app");
            application.setEnvironment("illegal-env");
        });
    }

    @Test
    void testRegistry() {
        ApplicationConfig application = new ApplicationConfig("app");
        RegistryConfig registry = new RegistryConfig();
        application.setRegistry(registry);
        MatcherAssert.assertThat(application.getRegistry(), Matchers.sameInstance(registry));
        application.setRegistries(Collections.singletonList(registry));
        MatcherAssert.assertThat(application.getRegistries(), Matchers.contains(registry));
        MatcherAssert.assertThat(application.getRegistries(), IsCollectionWithSize.hasSize(1));
    }

    @Test
    void testMonitor() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setMonitor(new MonitorConfig("monitor-addr"));
        MatcherAssert.assertThat(application.getMonitor().getAddress(), Matchers.equalTo("monitor-addr"));
        application.setMonitor("monitor-addr");
        MatcherAssert.assertThat(application.getMonitor().getAddress(), Matchers.equalTo("monitor-addr"));
    }

    @Test
    void testLogger() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setLogger("log4j");
        MatcherAssert.assertThat(application.getLogger(), Matchers.equalTo("log4j"));
    }

    @Test
    void testDefault() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setDefault(true);
        MatcherAssert.assertThat(application.isDefault(), Matchers.is(true));
    }

    @Test
    void testDumpDirectory() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setDumpDirectory("/dump");
        MatcherAssert.assertThat(application.getDumpDirectory(), Matchers.equalTo("/dump"));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(DUMP_DIRECTORY, "/dump"));
    }

    @Test
    void testQosEnable() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosEnable(true);
        MatcherAssert.assertThat(application.getQosEnable(), Matchers.is(true));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(QOS_ENABLE, "true"));
    }

    @Test
    void testQosPort() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosPort(8080);
        MatcherAssert.assertThat(application.getQosPort(), Matchers.equalTo(8080));
    }

    @Test
    void testQosAcceptForeignIp() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosAcceptForeignIp(true);
        MatcherAssert.assertThat(application.getQosAcceptForeignIp(), Matchers.is(true));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(ACCEPT_FOREIGN_IP, "true"));
    }

    @Test
    void testParameters() throws Exception {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosAcceptForeignIp(true);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("k1", "v1");
        ApplicationConfig.appendParameters(parameters, application);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry("k1", "v1"));
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(ACCEPT_FOREIGN_IP, "true"));
    }

    @Test
    void testAppendEnvironmentProperties() {
        ApplicationConfig application = new ApplicationConfig("app");
        SysProps.setProperty("dubbo.labels", "tag1=value1;tag2=value2 ; tag3 = value3");
        application.refresh();
        Map<String, String> parameters = application.getParameters();
        Assertions.assertEquals("value1", parameters.get("tag1"));
        Assertions.assertEquals("value2", parameters.get("tag2"));
        Assertions.assertEquals("value3", parameters.get("tag3"));

        ApplicationConfig application1 = new ApplicationConfig("app");
        SysProps.setProperty("dubbo.env.keys", "tag1, tag2,tag3");
        // mock environment variables
        SysProps.setProperty("tag1", "value1");
        SysProps.setProperty("tag2", "value2");
        SysProps.setProperty("tag3", "value3");
        application1.refresh();
        Map<String, String> parameters1 = application1.getParameters();
        Assertions.assertEquals("value2", parameters1.get("tag2"));
        Assertions.assertEquals("value3", parameters1.get("tag3"));

        Map<String, String> urlParameters = new HashMap<>();
        ApplicationConfig.appendParameters(urlParameters, application1);
        Assertions.assertEquals("value1", urlParameters.get("tag1"));
        Assertions.assertEquals("value2", urlParameters.get("tag2"));
        Assertions.assertEquals("value3", urlParameters.get("tag3"));
    }

    @Test
    void testMetaData() {
        ApplicationConfig config = new ApplicationConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }

    @Test
    void testOverrideEmptyConfig() {
        String owner = "tom1";
        SysProps.setProperty("dubbo.application.name", "demo-app");
        SysProps.setProperty("dubbo.application.owner", owner);
        SysProps.setProperty("dubbo.application.version", "1.2.3");

        ApplicationConfig applicationConfig = new ApplicationConfig();

        DubboBootstrap.getInstance()
            .application(applicationConfig)
            .initialize();

        Assertions.assertEquals(owner, applicationConfig.getOwner());
        Assertions.assertEquals("1.2.3", applicationConfig.getVersion());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    void testOverrideConfigById() {
        String owner = "tom2";
        SysProps.setProperty("dubbo.applications.demo-app.owner", owner);
        SysProps.setProperty("dubbo.applications.demo-app.version", "1.2.3");
        SysProps.setProperty("dubbo.applications.demo-app.name", "demo-app");

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setId("demo-app");

        DubboBootstrap.getInstance()
                .application(applicationConfig)
                .initialize();

        Assertions.assertEquals("demo-app", applicationConfig.getId());
        Assertions.assertEquals("demo-app", applicationConfig.getName());
        Assertions.assertEquals(owner, applicationConfig.getOwner());
        Assertions.assertEquals("1.2.3", applicationConfig.getVersion());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    void testOverrideConfigByName() {
        String owner = "tom3";
        SysProps.setProperty("dubbo.applications.demo-app.owner", owner);
        SysProps.setProperty("dubbo.applications.demo-app.version", "1.2.3");

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-app");

        DubboBootstrap.getInstance()
                .application(applicationConfig)
                .initialize();

        Assertions.assertEquals(owner, applicationConfig.getOwner());
        Assertions.assertEquals("1.2.3", applicationConfig.getVersion());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    void testLoadConfig() {
        String owner = "tom4";
        SysProps.setProperty("dubbo.applications.demo-app.owner", owner);
        SysProps.setProperty("dubbo.applications.demo-app.version", "1.2.3");

        DubboBootstrap.getInstance()
                .initialize();

        ApplicationConfig applicationConfig = DubboBootstrap.getInstance().getApplication();

        Assertions.assertEquals("demo-app", applicationConfig.getId());
        Assertions.assertEquals("demo-app", applicationConfig.getName());
        Assertions.assertEquals(owner, applicationConfig.getOwner());
        Assertions.assertEquals("1.2.3", applicationConfig.getVersion());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    void testOverrideConfigConvertCase() {
        SysProps.setProperty("dubbo.application.NAME", "demo-app");
        SysProps.setProperty("dubbo.application.qos-Enable", "false");
        SysProps.setProperty("dubbo.application.qos_host", "127.0.0.1");
        SysProps.setProperty("dubbo.application.qosPort", "2345");

        DubboBootstrap.getInstance()
                .initialize();

        ApplicationConfig applicationConfig = DubboBootstrap.getInstance().getApplication();

        Assertions.assertEquals(false, applicationConfig.getQosEnable());
        Assertions.assertEquals("127.0.0.1", applicationConfig.getQosHost());
        Assertions.assertEquals(2345, applicationConfig.getQosPort());
        Assertions.assertEquals("demo-app", applicationConfig.getName());

        DubboBootstrap.getInstance().destroy();
    }

    @Test
    void testDefaultValue() {
        SysProps.setProperty("dubbo.application.NAME", "demo-app");

        DubboBootstrap.getInstance()
            .initialize();

        ApplicationConfig applicationConfig = DubboBootstrap.getInstance().getApplication();

        Assertions.assertEquals(DUBBO, applicationConfig.getProtocol());
        Assertions.assertEquals(EXECUTOR_MANAGEMENT_MODE_ISOLATION, applicationConfig.getExecutorManagementMode());
        Assertions.assertEquals(Boolean.TRUE, applicationConfig.getEnableFileCache());

        DubboBootstrap.getInstance().destroy();
    }
}
