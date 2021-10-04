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

import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ModuleBuilderTest {

    @Test
    void name() {
        ModuleBuilder builder = new ModuleBuilder();
        builder.name("name");
        Assertions.assertEquals("name", builder.build().getName());
    }

    @Test
    void version() {
        ModuleBuilder builder = new ModuleBuilder();
        builder.version("version");
        Assertions.assertEquals("version", builder.build().getVersion());
    }

    @Test
    void owner() {
        ModuleBuilder builder = new ModuleBuilder();
        builder.owner("owner");
        Assertions.assertEquals("owner", builder.build().getOwner());
    }

    @Test
    void organization() {
        ModuleBuilder builder = new ModuleBuilder();
        builder.organization("organization");
        Assertions.assertEquals("organization", builder.build().getOrganization());
    }

    @Test
    void addRegistries() {
        RegistryConfig registry = new RegistryConfig();
        ModuleBuilder builder = new ModuleBuilder();
        builder.addRegistries(Collections.singletonList(registry));
        Assertions.assertTrue(builder.build().getRegistries().contains(registry));
        Assertions.assertEquals(1, builder.build().getRegistries().size());
    }

    @Test
    void addRegistry() {
        RegistryConfig registry = new RegistryConfig();
        ModuleBuilder builder = new ModuleBuilder();
        builder.addRegistry(registry);
        Assertions.assertTrue(builder.build().getRegistries().contains(registry));
        Assertions.assertEquals(1, builder.build().getRegistries().size());
    }

    @Test
    void monitor() {
        MonitorConfig monitor = new MonitorConfig();
        ModuleBuilder builder = new ModuleBuilder();
        builder.monitor(monitor);
        Assertions.assertSame(monitor, builder.build().getMonitor());
    }

    @Test
    void isDefault() {
        ModuleBuilder builder = new ModuleBuilder();
        builder.isDefault(true);
        Assertions.assertTrue(builder.build().isDefault());
    }

    @Test
    void build() {
        RegistryConfig registry = new RegistryConfig();
        MonitorConfig monitor = new MonitorConfig();

        ModuleBuilder builder = new ModuleBuilder();
        builder.name("name").version("version").owner("owner").organization("organization").addRegistry(registry)
                .monitor(monitor).isDefault(false);

        ModuleConfig config = builder.build();
        ModuleConfig config2 = builder.build();

        Assertions.assertEquals("name", config.getName());
        Assertions.assertEquals("version", config.getVersion());
        Assertions.assertEquals("owner", config.getOwner());
        Assertions.assertEquals("organization", config.getOrganization());
        Assertions.assertTrue(builder.build().getRegistries().contains(registry));
        Assertions.assertSame(monitor, builder.build().getMonitor());
        Assertions.assertFalse(config.isDefault());
        Assertions.assertNotSame(config, config2);
    }
}
