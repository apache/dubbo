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
package org.apache.dubbo.config.context;

import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link ConfigConfigurationAdapter}
 */
public class ConfigConfigurationAdapterTest {

    @Test
    public void test() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("127.0.0.1");
        registryConfig.setPort(2181);

        String prefix = "dubbo.registry";
        ConfigConfigurationAdapter configConfigurationAdapter = new ConfigConfigurationAdapter(registryConfig, prefix);

        Assertions.assertEquals(configConfigurationAdapter.getInternalProperty(prefix + "." + "address"), "127.0.0.1");
        Assertions.assertEquals(configConfigurationAdapter.getInternalProperty(prefix + "." + "port"), "2181");
    }
}
