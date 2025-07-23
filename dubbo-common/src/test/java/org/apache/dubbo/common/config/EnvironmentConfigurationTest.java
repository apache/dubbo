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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Environment configuration test.
 */
class EnvironmentConfigurationTest {

    private static final String MOCK_KEY = "DUBBO_KEY";
    private static final String MOCK_VALUE = "mockValue";

    private EnvironmentConfiguration envConfig(Map<String, String> map) {
        return new EnvironmentConfiguration() {
            @Override
            protected Map<String, String> getenv() {
                return map;
            }
        };
    }

    @Test
    void testGetInternalProperty() {
        Map<String, String> map = new HashMap<>();
        map.put(MOCK_KEY, MOCK_VALUE);
        EnvironmentConfiguration configuration = envConfig(map);
        Assertions.assertEquals(MOCK_VALUE, configuration.getInternalProperty("dubbo.key"));
        Assertions.assertEquals(MOCK_VALUE, configuration.getInternalProperty("key"));
        Assertions.assertEquals(MOCK_VALUE, configuration.getInternalProperty("dubbo_key"));
        Assertions.assertEquals(MOCK_VALUE, configuration.getInternalProperty(MOCK_KEY));
    }

    @Test
    void testGetProperties() {
        Map<String, String> map = new HashMap<>();
        map.put(MOCK_KEY, MOCK_VALUE);
        EnvironmentConfiguration configuration = new EnvironmentConfiguration() {
            @Override
            protected Map<String, String> getenv() {
                return map;
            }
        };
        Assertions.assertEquals(map, configuration.getProperties());
    }

    @Test
    void testHyphenAndDotKeyResolveFromEnv() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("DUBBO_ABC_DEF_GHI", "v1");
        envMap.put("DUBBO_ABCDEF_GHI", "v2");
        envMap.put("DUBBO_ABC-DEF_GHI", "v3");
        envMap.put("dubbo_abc_def_ghi", "v4");

        EnvironmentConfiguration configuration = envConfig(envMap);

        String dubboKey = "dubbo.abc-def.ghi";

        Assertions.assertEquals("v1", configuration.getProperty(dubboKey));

        envMap.remove("DUBBO_ABC_DEF_GHI");
        configuration = envConfig(envMap);
        Assertions.assertEquals("v2", configuration.getProperty(dubboKey));

        envMap.remove("DUBBO_ABCDEF_GHI");
        configuration = envConfig(envMap);
        Assertions.assertEquals("v3", configuration.getProperty(dubboKey));

        envMap.remove("DUBBO_ABC-DEF_GHI");
        configuration = envConfig(envMap);
        Assertions.assertEquals("v4", configuration.getProperty(dubboKey));

        envMap.remove("dubbo_abc_def_ghi");
        configuration = envConfig(envMap);
        Assertions.assertNull(configuration.getProperty(dubboKey));
    }
}
