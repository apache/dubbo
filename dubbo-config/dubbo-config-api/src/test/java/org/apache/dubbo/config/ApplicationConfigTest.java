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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_ISOLATION;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

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
        assertThat(application.getName(), equalTo("app"));
        assertThat(application.getId(), equalTo(null));

        application = new ApplicationConfig("app2");
        assertThat(application.getName(), equalTo("app2"));
        assertThat(application.getId(), equalTo(null));

        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        assertThat(parameters, hasEntry(APPLICATION_KEY, "app2"));
    }

    @Test
    void testVersion() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setVersion("1.0.0");
        assertThat(application.getVersion(), equalTo("1.0.0"));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        assertThat(parameters, hasEntry("application.version", "1.0.0"));
    }

    @Test
    void testOwner() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setOwner("owner");
        assertThat(application.getOwner(), equalTo("owner"));
    }

    @Test
    void testOrganization() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setOrganization("org");
        assertThat(application.getOrganization(), equalTo("org"));
    }

    @Test
    void testArchitecture() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setArchitecture("arch");
        assertThat(application.getArchitecture(), equalTo("arch"));
    }

    @Test
    void testEnvironment1() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setEnvironment("develop");
        assertThat(application.getEnvironment(), equalTo("develop"));
        application.setEnvironment("test");
        assertThat(application.getEnvironment(), equalTo("test"));
        application.setEnvironment("product");
        assertThat(application.getEnvironment(), equalTo("product"));
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
        assertThat(application.getRegistry(), sameInstance(registry));
        application.setRegistries(Collections.singletonList(registry));
        assertThat(application.getRegistries(), contains(registry));
        assertThat(application.getRegistries(), hasSize(1));
    }

    @Test
    void testMonitor() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setMonitor(new MonitorConfig("monitor-addr"));
        assertThat(application.getMonitor().getAddress(), equalTo("monitor-addr"));
        application.setMonitor("monitor-addr");
        assertThat(application.getMonitor().getAddress(), equalTo("monitor-addr"));
    }

    @Test
    void testLogger() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setLogger("log4j");
        assertThat(application.getLogger(), equalTo("log4j"));
    }

    @Test
    void testDefault() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setDefault(true);
        assertThat(application.isDefault(), is(true));
    }

    @Test
    void testDumpDirectory() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setDumpDirectory("/dump");
        assertThat(application.getDumpDirectory(), equalTo("/dump"));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        assertThat(parameters, hasEntry(DUMP_DIRECTORY, "/dump"));
    }

    @Test
    void testQosEnable() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosEnable(true);
        assertThat(application.getQosEnable(), is(true));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        assertThat(parameters, hasEntry(QOS_ENABLE, "true"));
    }

    @Test
    void testQosPort() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosPort(8080);
        assertThat(application.getQosPort(), equalTo(8080));
    }

    @Test
    void testQosAcceptForeignIp() {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosAcceptForeignIp(true);
        assertThat(application.getQosAcceptForeignIp(), is(true));
        Map<String, String> parameters = new HashMap<String, String>();
        ApplicationConfig.appendParameters(parameters, application);
        assertThat(parameters, hasEntry(ACCEPT_FOREIGN_IP, "true"));
    }

    @Test
    void testParameters() throws Exception {
        ApplicationConfig application = new ApplicationConfig("app");
        application.setQosAcceptForeignIp(true);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("k1", "v1");
        ApplicationConfig.appendParameters(parameters, application);
        assertThat(parameters, hasEntry("k1", "v1"));
        assertThat(parameters, hasEntry(ACCEPT_FOREIGN_IP, "true"));
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
