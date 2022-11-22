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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

class ModuleConfigTest {

    @Test
    void testName2() throws Exception {
        ModuleConfig module = new ModuleConfig();
        module.setName("module-name");
        assertThat(module.getName(), equalTo("module-name"));
        assertThat(module.getId(), equalTo(null));
        Map<String, String> parameters = new HashMap<String, String>();
        ModuleConfig.appendParameters(parameters, module);
        assertThat(parameters, hasEntry("module", "module-name"));
    }

    @Test
    void testVersion() throws Exception {
        ModuleConfig module = new ModuleConfig();
        module.setName("module-name");
        module.setVersion("1.0.0");
        assertThat(module.getVersion(), equalTo("1.0.0"));
        Map<String, String> parameters = new HashMap<String, String>();
        ModuleConfig.appendParameters(parameters, module);
        assertThat(parameters, hasEntry("module.version", "1.0.0"));
    }

    @Test
    void testOwner() throws Exception {
        ModuleConfig module = new ModuleConfig();
        module.setOwner("owner");
        assertThat(module.getOwner(), equalTo("owner"));
    }

    @Test
    void testOrganization() throws Exception {
        ModuleConfig module = new ModuleConfig();
        module.setOrganization("org");
        assertThat(module.getOrganization(), equalTo("org"));
    }

    @Test
    void testRegistry() throws Exception {
        ModuleConfig module = new ModuleConfig();
        RegistryConfig registry = new RegistryConfig();
        module.setRegistry(registry);
        assertThat(module.getRegistry(), sameInstance(registry));
    }

    @Test
    void testRegistries() throws Exception {
        ModuleConfig module = new ModuleConfig();
        RegistryConfig registry = new RegistryConfig();
        module.setRegistries(Collections.singletonList(registry));
        assertThat(module.getRegistries(), Matchers.<RegistryConfig>hasSize(1));
        assertThat(module.getRegistries(), contains(registry));
    }

    @Test
    void testMonitor() throws Exception {
        ModuleConfig module = new ModuleConfig();
        module.setMonitor("monitor-addr1");
        assertThat(module.getMonitor().getAddress(), equalTo("monitor-addr1"));
        module.setMonitor(new MonitorConfig("monitor-addr2"));
        assertThat(module.getMonitor().getAddress(), equalTo("monitor-addr2"));
    }

    @Test
    void testDefault() throws Exception {
        ModuleConfig module = new ModuleConfig();
        module.setDefault(true);
        assertThat(module.isDefault(), is(true));
    }

    @Test
    void testMetaData() {
        MonitorConfig config = new MonitorConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }
}