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
package org.apache.dubbo.config.spring.schema.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ConfigCenterBean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * {@link ParameterizedDubboConfigBeanDefinitionParser} Test cases
 *
 * @see ParameterizedDubboConfigBeanDefinitionParser
 * @see ApplicationConfig
 * @since 2.7.7
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ParameterizedDubboConfigBeanDefinitionParserTest.class)
@TestPropertySource(properties = {
        "a = 1",
        "b = 2",
        "dubbo.monitor.address = zookeeper://127.0.0.1:2181",
        "dubbo.config-center.address = zookeeper://localhost:2181",
        "dubbo.metadata-center.address = zookeeper://localhost:2181",
        "user.name = mercyblitz",
        "user.password = ******",
        "timeout = 60"
})
@ImportResource(locations = "classpath:/META-INF/spring/parameterized-dubbo-config-bean-definition-parser-test.xml")
public class ParameterizedDubboConfigBeanDefinitionParserTest {

    @Autowired
    private Map<String, ApplicationConfig> applicationConfigsMap;

    @Autowired
    @Qualifier("dubbo-monitor-test")
    private MonitorConfig monitorConfig;

    @Autowired
    @Qualifier("dubbo-config-center-test")
    private ConfigCenterBean configCenterBean;

    @Autowired
    @Qualifier("dubbo-metadata-report-test")
    private MetadataReportConfig metadataReportConfig;

    @Autowired
    @Qualifier("dubbo-protocol-test")
    private ProtocolConfig protocolConfig;

    @Autowired
    @Qualifier("dubbo-registry-test")
    private RegistryConfig registryConfig;

    @Test
    public void test() {
        testApplicationConfig();
        testMonitorConfig();
        testConfigCenterConfig();
        testMetadataReportConfig();
        testProtocolConfig();
        testRegistryConfig();
    }

    /**
     * name="dubbo-application-test"
     * version="${a}.${b}"
     * owner="mercyblitz"
     * organization="Apache"
     * architecture="Dubbo"
     * environment="product"
     * compiler="jdk"
     * logger="slf4j"
     * registry="mockRegistry"
     * monitor="dubbo-monitor-test"
     * default="true"
     * metadata-type="local"
     * register-consumer="false"
     */
    private void testApplicationConfig() {
        ApplicationConfig applicationConfig = applicationConfigsMap.get("dubbo-application-test");
        assertEquals("dubbo-application-test", applicationConfig.getName());
        assertEquals("1.2", applicationConfig.getVersion());
        assertEquals("mercyblitz", applicationConfig.getOwner());
        assertEquals("Apache", applicationConfig.getOrganization());
        assertEquals("Dubbo", applicationConfig.getArchitecture());
        assertEquals("product", applicationConfig.getEnvironment());
        assertEquals("jdk", applicationConfig.getCompiler());
        assertEquals("slf4j", applicationConfig.getLogger());
        assertEquals(registryConfig, applicationConfig.getRegistry());
        assertEquals(monitorConfig, applicationConfig.getMonitor());
        assertEquals(Boolean.TRUE, applicationConfig.isDefault());
        assertEquals("local", applicationConfig.getMetadataType());
        assertEquals(Boolean.FALSE, applicationConfig.getRegisterConsumer());
        assertEquals("1", applicationConfig.getParameter("a"));
        assertEquals("2", applicationConfig.getParameter("b"));
        assertEquals("3", applicationConfig.getParameter("c"));
    }

    /**
     * id="dubbo-monitor-test"
     * address="${dubbo.monitor.address}"
     * protocol="dubbo"
     * username="${user.name}"
     * password="${user.password}"
     * group="default"
     * version="${a}.${b}"
     * interval="30"
     * default="true"
     */
    private void testMonitorConfig() {
        assertEquals("dubbo-monitor-test", monitorConfig.getId());
        assertEquals("zookeeper://127.0.0.1:2181", monitorConfig.getAddress());
        assertEquals("dubbo", monitorConfig.getProtocol());
        assertEquals("mercyblitz", monitorConfig.getUsername());
        assertEquals("******", monitorConfig.getPassword());
        assertEquals("default", monitorConfig.getGroup());
        assertEquals("1.2", monitorConfig.getVersion());
        assertEquals("30", monitorConfig.getInterval());
        assertEquals(Boolean.TRUE, monitorConfig.isDefault());
        assertEquals("mercyblitz", monitorConfig.getParameter("userName"));
    }

    /**
     * id="dubbo-config-center-test"
     * protocol="dubbo"
     * address="${dubbo.config-center.address}"
     * cluster="c1"
     * namespace="n1"
     * group="default"
     * config-file="/home/mercybltz/dubbo/config-file"
     * app-config-file="/home/mercybltz/dubbo/app-config-config"
     * username="${user.name}"
     * password="${user.password}"
     * timeout="${timeout}"
     * highest-priority="true"
     * include-spring-env="true"
     * check="true"
     */
    private void testConfigCenterConfig() {
        assertEquals("dubbo-config-center-test", configCenterBean.getId());
        assertEquals("dubbo", configCenterBean.getProtocol());
        assertEquals("zookeeper://localhost:2181", configCenterBean.getAddress());
        assertEquals("c1", configCenterBean.getCluster());
        assertEquals("n1", configCenterBean.getNamespace());
        assertEquals("default", configCenterBean.getGroup());
        assertEquals("/home/mercybltz/dubbo/config-file", configCenterBean.getConfigFile());
        assertEquals("/home/mercybltz/dubbo/app-config-config", configCenterBean.getAppConfigFile());
        assertEquals("mercyblitz", configCenterBean.getUsername());
        assertEquals("******", configCenterBean.getPassword());
        assertEquals(Long.valueOf(60), configCenterBean.getTimeout());
        assertEquals(Boolean.TRUE, configCenterBean.isHighestPriority());
        assertEquals(Boolean.TRUE, configCenterBean.getIncludeSpringEnv());
        assertEquals(Boolean.TRUE, configCenterBean.isCheck());
        assertEquals("mercyblitz", configCenterBean.getParameter("user.name"));

    }

    /**
     * id="dubbo-metadata-report-test"
     * address="${dubbo.metadata-center.address}"
     * username="${user.name}"
     * password="${user.password}"
     * timeout="${timeout}"
     * group="metadata"
     * retry-times="1"
     * retry-period="2"
     * cycle-report="true"
     * sync-report="true"
     * cluster="true"
     */
    private void testMetadataReportConfig() {
        assertEquals("dubbo-metadata-report-test", metadataReportConfig.getId());
        assertEquals("zookeeper://localhost:2181", metadataReportConfig.getAddress());
        assertEquals("mercyblitz", metadataReportConfig.getUsername());
        assertEquals("******", metadataReportConfig.getPassword());
        assertEquals(Long.valueOf(60), configCenterBean.getTimeout());
        assertEquals("metadata", metadataReportConfig.getGroup());
        assertEquals(Integer.valueOf(1), metadataReportConfig.getRetryTimes());
        assertEquals(Integer.valueOf(2), metadataReportConfig.getRetryPeriod());
        assertEquals(Boolean.TRUE, metadataReportConfig.getCluster());
        assertEquals(Boolean.TRUE, metadataReportConfig.getCycleReport());
        assertEquals(Boolean.TRUE, metadataReportConfig.getSyncReport());
        assertEquals("xiaomage", metadataReportConfig.getParameter("user.name"));
    }

    /**
     * id="dubbo-protocol-test"
     * name="dubbo"
     * host="127.0.0.1"
     * port="20880"
     * threadpool="fixed"
     * threadname="thread-pool-name"
     * threads="100"
     * corethreads="20"
     * iothreads="30"
     * queues="99999"
     * accepts="20"
     * codec="netty"
     * serialization="hession"
     * keepalive="true"
     * optimizer="my-optimizer"
     * extension="none"
     * charset="UTF-8"
     * payload="12345"
     * buffer="1024"
     * heartbeat="30"
     * accesslog="/home/mercyblitz/dubbo/access-log.log"
     * telnet="qos"
     * prompt="Hello,World"
     * status="UP"
     * transporter="netty"
     * exchanger="abc"
     * dispatcher="dispatcher"
     * networker="networker"
     * server="server"
     * client="client"
     * contextpath="/home/mercyblitz/dubbo/"
     * register="true"
     * default="true"
     * ssl-enabled="true"
     */
    private void testProtocolConfig() {
        assertEquals("dubbo-protocol-test", protocolConfig.getId());
        assertEquals("dubbo", protocolConfig.getName());
        assertEquals("127.0.0.1", protocolConfig.getHost());
        assertEquals(Integer.valueOf(20880), protocolConfig.getPort());
        assertEquals("fixed", protocolConfig.getThreadpool());
        assertEquals("thread-pool-name", protocolConfig.getThreadname());
        assertEquals(Integer.valueOf(100), protocolConfig.getThreads());
        assertEquals(Integer.valueOf(20), protocolConfig.getCorethreads());
        assertEquals(Integer.valueOf(30), protocolConfig.getIothreads());
        assertEquals(Integer.valueOf(99999), protocolConfig.getQueues());
        assertEquals(Integer.valueOf(20), protocolConfig.getAccepts());
        assertEquals("netty", protocolConfig.getCodec());
        assertEquals("hession", protocolConfig.getSerialization());
        assertEquals(Boolean.TRUE, protocolConfig.getKeepAlive());
        assertEquals("my-optimizer", protocolConfig.getOptimizer());
        assertEquals("none", protocolConfig.getExtension());
        assertEquals("UTF-8", protocolConfig.getCharset());
        assertEquals(Integer.valueOf(12345), protocolConfig.getPayload());
        assertEquals(Integer.valueOf(1024), protocolConfig.getBuffer());
        assertEquals(Integer.valueOf(30), protocolConfig.getHeartbeat());
        assertEquals("/home/mercyblitz/dubbo/access-log.log", protocolConfig.getAccesslog());
        assertEquals("qos", protocolConfig.getTelnet());
        assertEquals("Hello,World", protocolConfig.getPrompt());
        assertEquals("UP", protocolConfig.getStatus());
        assertEquals("netty", protocolConfig.getTransporter());
        assertEquals("abc", protocolConfig.getExchanger());
        assertEquals("dispatcher", protocolConfig.getDispatcher());
        assertEquals("networker", protocolConfig.getNetworker());
        assertEquals("server", protocolConfig.getServer());
        assertEquals("client", protocolConfig.getClient());
        assertEquals("/home/mercyblitz/dubbo/", protocolConfig.getPath());
        assertEquals("/home/mercyblitz/dubbo/", protocolConfig.getContextpath());
        assertEquals(Boolean.TRUE, protocolConfig.isRegister());
        assertEquals(Boolean.TRUE, protocolConfig.isDefault());
        assertEquals(Boolean.TRUE, protocolConfig.getSslEnabled());
        assertEquals("1", protocolConfig.getParameter("a"));
    }

    /**
     * id="dubbo-registry-test"
     * address="zookeeper://127.0.0.1:2181"
     * port="2181"
     * protocol="zookeeper"
     * username="${user.name}"
     * password="${user.password}"
     * transport="netty"
     * transporter="netty"
     * server="server"
     * client="client"
     * cluster="cluster"
     * zone="zone"
     * forks="forks"
     * group="default"
     * version="${a}.${b}"
     * timeout="${timeout}"
     * session="${timeout}"
     * file="/home/mercyblitz/dubbo/registry"
     * wait="999"
     * check="true"
     * dynamic="true"
     * register="true"
     * subscribe="true"
     * default="true"
     * simplified="true"
     * extra-keys="a,b,c"
     * use-as-config-center="true"
     * use-as-metadata-center="true"
     * accepts="9"
     * preferred="true"
     * weight="99"
     */
    private void testRegistryConfig() {
        assertEquals("dubbo-registry-test", registryConfig.getId());
        assertEquals("zookeeper://127.0.0.1:2181", registryConfig.getAddress());
        assertEquals(Integer.valueOf(2181), registryConfig.getPort());
        assertEquals("zookeeper", registryConfig.getProtocol());
        assertEquals("mercyblitz", registryConfig.getUsername());
        assertEquals("******", registryConfig.getPassword());
        assertEquals("netty", registryConfig.getTransport());
        assertEquals("netty", registryConfig.getTransporter());
        assertEquals("server", registryConfig.getServer());
        assertEquals("client", registryConfig.getClient());
        assertEquals("cluster", registryConfig.getCluster());
        assertEquals("zone", registryConfig.getZone());
        assertEquals("default", registryConfig.getGroup());
        assertEquals("1.2", registryConfig.getVersion());
        assertEquals(Integer.valueOf(60), registryConfig.getTimeout());
        assertEquals(Integer.valueOf(60), registryConfig.getSession());
        assertEquals("/home/mercyblitz/dubbo/registry", registryConfig.getFile());
        assertEquals(Integer.valueOf(999), registryConfig.getWait());
        assertEquals(Boolean.TRUE, registryConfig.isCheck());
        assertEquals(Boolean.TRUE, registryConfig.isDynamic());
        assertEquals(Boolean.TRUE, registryConfig.isRegister());
        assertEquals(Boolean.TRUE, registryConfig.isSubscribe());
        assertEquals(Boolean.TRUE, registryConfig.isDefault());
        assertEquals(Boolean.TRUE, registryConfig.getSimplified());
        assertEquals("a,b,c", registryConfig.getExtraKeys());
        assertEquals(Boolean.TRUE, registryConfig.getUseAsConfigCenter());
        assertEquals(Boolean.TRUE, registryConfig.getUseAsMetadataCenter());
        assertEquals("9", registryConfig.getAccepts());
        assertEquals(Boolean.TRUE, registryConfig.getPreferred());
        assertEquals(Integer.valueOf(99), registryConfig.getWeight());
    }
}
