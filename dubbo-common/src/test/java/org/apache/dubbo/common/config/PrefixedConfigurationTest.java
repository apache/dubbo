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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

class PrefixedConfigurationTest {

    @Test
    void testPrefixedConfiguration() {
        Map<String,String> props = new LinkedHashMap<>();
        props.put("dubbo.protocol.name", "dubbo");
        props.put("dubbo.protocol.port", "1234");
        props.put("dubbo.protocols.rest.port", "2345");
        InmemoryConfiguration inmemoryConfiguration = new InmemoryConfiguration();
        inmemoryConfiguration.addProperties(props);

        // prefixed over InmemoryConfiguration
        PrefixedConfiguration prefixedConfiguration = new PrefixedConfiguration(inmemoryConfiguration, "dubbo.protocol");
        Assertions.assertEquals("dubbo", prefixedConfiguration.getProperty("name"));
        Assertions.assertEquals("1234", prefixedConfiguration.getProperty("port"));

        prefixedConfiguration = new PrefixedConfiguration(inmemoryConfiguration, "dubbo.protocols.rest");
        Assertions.assertEquals("2345", prefixedConfiguration.getProperty("port"));

        // prefixed over composite configuration
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(inmemoryConfiguration);
        prefixedConfiguration = new PrefixedConfiguration(compositeConfiguration, "dubbo.protocols.rest");
        Assertions.assertEquals("2345", prefixedConfiguration.getProperty("port"));

    }
}