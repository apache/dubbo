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
package org.apache.dubbo.common.config;

import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * {@link PropertiesConfiguration}
 */
class PropertiesConfigurationTest {

    @Test
    void test() {
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(ApplicationModel.defaultModel());

        Map<String, String> properties = propertiesConfiguration.getProperties();
        Assertions.assertEquals(properties.get("dubbo"), "properties");
        Assertions.assertEquals(properties.get("dubbo.application.enable-file-cache"), "false");
        Assertions.assertEquals(properties.get("dubbo.service.shutdown.wait"), "200");

        Assertions.assertEquals(propertiesConfiguration.getProperty("dubbo"), "properties");
        Assertions.assertEquals(propertiesConfiguration.getInternalProperty("dubbo"), "properties");

        propertiesConfiguration.setProperty("k1", "v1");
        Assertions.assertEquals(propertiesConfiguration.getProperty("k1"), "v1");
        propertiesConfiguration.remove("k1");
        Assertions.assertNull(propertiesConfiguration.getProperty("k1"));
    }
}