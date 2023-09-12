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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ApplicationBuilderTest {

    @Test
    void name() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.name("app");
        Assertions.assertEquals("app", builder.build().getName());
    }

    @Test
    void version() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.version("version");
        Assertions.assertEquals("version", builder.build().getVersion());
    }

    @Test
    void owner() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.owner("owner");
        Assertions.assertEquals("owner", builder.build().getOwner());
    }

    @Test
    void organization() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.organization("organization");
        Assertions.assertEquals("organization", builder.build().getOrganization());
    }

    @Test
    void architecture() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.architecture("architecture");
        Assertions.assertEquals("architecture", builder.build().getArchitecture());
    }

    @Test
    void environment() {
        ApplicationBuilder builder = new ApplicationBuilder();
        Assertions.assertEquals("product", builder.build().getEnvironment());
        builder.environment("develop");
        Assertions.assertEquals("develop", builder.build().getEnvironment());
        builder.environment("test");
        Assertions.assertEquals("test", builder.build().getEnvironment());
        builder.environment("product");
        Assertions.assertEquals("product", builder.build().getEnvironment());
    }

    @Test
    void compiler() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.compiler("compiler");
        Assertions.assertEquals("compiler", builder.build().getCompiler());
    }

    @Test
    void logger() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.logger("log4j");
        Assertions.assertEquals("log4j", builder.build().getLogger());
    }

    @Test
    void addRegistry() {
        RegistryConfig registry = new RegistryConfig();
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.addRegistry(registry);
        Assertions.assertNotNull(builder.build().getRegistry());
        Assertions.assertEquals(1, builder.build().getRegistries().size());
        Assertions.assertSame(registry, builder.build().getRegistry());
    }

    @Test
    void addRegistries() {
        RegistryConfig registry = new RegistryConfig();
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.addRegistries(Collections.singletonList(registry));
        Assertions.assertNotNull(builder.build().getRegistry());
        Assertions.assertEquals(1, builder.build().getRegistries().size());
        Assertions.assertSame(registry, builder.build().getRegistry());
    }

    @Test
    void registryIds() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.registryIds("registryIds");
        Assertions.assertEquals("registryIds", builder.build().getRegistryIds());
    }

    @Test
    void monitor() {
        MonitorConfig monitor = new MonitorConfig("monitor-addr");
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.monitor(monitor);
        Assertions.assertSame(monitor, builder.build().getMonitor());
        Assertions.assertEquals("monitor-addr", builder.build().getMonitor().getAddress());
    }

    @Test
    void monitor1() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.monitor("monitor-addr");
        Assertions.assertEquals("monitor-addr", builder.build().getMonitor().getAddress());
    }

    @Test
    void isDefault() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.isDefault(true);
        Assertions.assertTrue(builder.build().isDefault());
        builder.isDefault(false);
        Assertions.assertFalse(builder.build().isDefault());
        builder.isDefault(null);
        Assertions.assertNull(builder.build().isDefault());
    }

    @Test
    void dumpDirectory() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.dumpDirectory("dumpDirectory");
        Assertions.assertEquals("dumpDirectory", builder.build().getDumpDirectory());
    }

    @Test
    void qosEnable() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.qosEnable(true);
        Assertions.assertTrue(builder.build().getQosEnable());
        builder.qosEnable(false);
        Assertions.assertFalse(builder.build().getQosEnable());
        builder.qosEnable(null);
        Assertions.assertNull(builder.build().getQosEnable());
    }

    @Test
    void qosPort() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.qosPort(8080);
        Assertions.assertEquals(8080, builder.build().getQosPort());
    }

    @Test
    void qosAcceptForeignIp() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.qosAcceptForeignIp(true);
        Assertions.assertTrue(builder.build().getQosAcceptForeignIp());
        builder.qosAcceptForeignIp(false);
        Assertions.assertFalse(builder.build().getQosAcceptForeignIp());
        builder.qosAcceptForeignIp(null);
        Assertions.assertNull(builder.build().getQosAcceptForeignIp());
    }

    @Test
    void shutwait() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.shutwait("shutwait");
        Assertions.assertEquals("shutwait", builder.build().getShutwait());
    }

    @Test
    void appendParameter() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.appendParameter("default.num", "one").appendParameter("num", "ONE");

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void appendParameters() {
        Map<String, String> source = new HashMap<>();
        source.put("default.num", "one");
        source.put("num", "ONE");

        ApplicationBuilder builder = new ApplicationBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void metadataServicePort() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.metadataServicePort(12345);
        Assertions.assertEquals(12345, builder.build().getMetadataServicePort());
    }

    @Test
    void livenessProbe() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.livenessProbe("TestProbe");
        Assertions.assertEquals("TestProbe", builder.build().getLivenessProbe());
    }

    @Test
    void readinessProbe() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.readinessProbe("TestProbe");
        Assertions.assertEquals("TestProbe", builder.build().getReadinessProbe());
    }

    @Test
    void startupProbe() {
        ApplicationBuilder builder = new ApplicationBuilder();
        builder.startupProbe("TestProbe");
        Assertions.assertEquals("TestProbe", builder.build().getStartupProbe());
    }

    @Test
    void build() {
        MonitorConfig monitor = new MonitorConfig("monitor-addr");
        RegistryConfig registry = new RegistryConfig();

        ApplicationBuilder builder = new ApplicationBuilder();
        builder.id("id").name("name").version("version").owner("owner").organization("organization").architecture("architecture")
                .environment("develop").compiler("compiler").logger("log4j").monitor(monitor).isDefault(false)
                .dumpDirectory("dumpDirectory").qosEnable(true).qosPort(8080).qosAcceptForeignIp(false)
                .shutwait("shutwait").registryIds("registryIds").addRegistry(registry)
                .appendParameter("default.num", "one").metadataServicePort(12345)
                .livenessProbe("liveness").readinessProbe("readiness").startupProbe("startup");

        ApplicationConfig config = builder.build();
        ApplicationConfig config2 = builder.build();

        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("name", config.getName());
        Assertions.assertEquals("version", config.getVersion());
        Assertions.assertEquals("owner", config.getOwner());
        Assertions.assertEquals("organization", config.getOrganization());
        Assertions.assertEquals("architecture", config.getArchitecture());
        Assertions.assertEquals("develop", config.getEnvironment());
        Assertions.assertEquals("compiler", config.getCompiler());
        Assertions.assertEquals("log4j", config.getLogger());
        Assertions.assertSame(monitor, config.getMonitor());
        Assertions.assertFalse(config.isDefault());
        Assertions.assertEquals("dumpDirectory", config.getDumpDirectory());
        Assertions.assertTrue(config.getQosEnable());
        Assertions.assertEquals(8080, config.getQosPort());
        Assertions.assertFalse(config.getQosAcceptForeignIp());
        Assertions.assertEquals("shutwait", config.getShutwait());
        Assertions.assertEquals("registryIds", config.getRegistryIds());
        Assertions.assertSame(registry, config.getRegistry());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));
        Assertions.assertEquals(12345, config.getMetadataServicePort());
        Assertions.assertEquals("liveness", config.getLivenessProbe());
        Assertions.assertEquals("readiness", config.getReadinessProbe());
        Assertions.assertEquals("startup", config.getStartupProbe());

        Assertions.assertNotSame(config, config2);
    }
}