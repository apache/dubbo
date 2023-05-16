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

import java.util.HashMap;
import java.util.Map;

/**
 * The type Environment configuration test.
 */
class EnvironmentConfigurationTest {

    private static final String MOCK_KEY = "DUBBO_KEY";
    private static final String MOCK_VALUE = "mockValue";

    @Test
    void testGetInternalProperty() {
        Map<String, String> map = new HashMap<>();
        map.put(MOCK_KEY, MOCK_VALUE);
        EnvironmentConfiguration configuration = new EnvironmentConfiguration() {
            @Override
            protected String getenv(String key) {
                return map.get(key);
            }
        };
        // this UT maybe only works on particular platform, assert only when value is not null.
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
}
